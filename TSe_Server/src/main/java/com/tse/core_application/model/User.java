package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.RegistrationRequest;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tse_users", schema= Constants.SCHEMA_NAME)
public class User implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="user_id", nullable = false)
    private Long userId;

	@Column(name = "primary_email")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=70)
	private String primaryEmail;

	@Column(name = "is_primary_email_personal")
	private Boolean isPrimaryEmailPersonal;

	@Column(name = "alternate_email")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=70)
	private String alternateEmail;

	@Column(name = "is_alternate_email_personal")
	private Boolean isAlternateEmailPersonal;

	@Column(name = "personal_email")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=70)
	private String personalEmail;

	@Column(name = "current_org_email")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max = 70)
	private String currentOrgEmail;

	@Column(name = "multi_association")
	private Boolean multiAssociation;

	@Column(name = "given_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String givenName;

	@Column(name = "first_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String firstName;

	@Column(name = "last_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String lastName;

	@Column(name = "middle_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String middleName;

	@Column(name = "locale")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String locale;

	@Column(name = "city")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String city;

	@Column(name = "highest_education")
    private Integer highestEducation;

	@Column(name = "second_highest_education")
    private String secondHighestEducation;

	@Column(name = "gender")
    private Integer gender;

	@Column(name = "age_range")
    private Integer ageRange;

	// new columns for openFire //
	@Column(name = "chat_user_name")
	@Convert(converter = DataEncryptionConverter.class)
	private String chatUserName;

	@Column(name = "chat_password")
	@Convert(converter = DataEncryptionConverter.class)
	private String chatPassword;
	// ------------------------ //


	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;

	@Column(name = "time_zone", nullable = false, length = 50)
	private String timeZone;

	@ManyToOne(optional = false)
	@JoinColumn(name = "country_id", referencedColumnName = "country_id")
	private Country fkCountryId;

	@Column(name = "image_data", length = 10000)
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=5000)
	private String imageData;

	@Column(name = "is_user_managing")
	private Boolean isUserManaging;

	@Column(name = "managing_user_id")
	private Long managingUserId;

//	@Override
//	public String toString() {
//		return "User{" +
//				"userId=" + userId +
//				", primaryEmail='" + primaryEmail + '\'' +
//				", isPrimaryEmailPersonal=" + isPrimaryEmailPersonal +
//				", alternateEmail='" + alternateEmail + '\'' +
//				", isAlternateEmailPersonal=" + isAlternateEmailPersonal +
//				", personalEmail='" + personalEmail + '\'' +
//				", currentOrgEmail='" + currentOrgEmail + '\'' +
//				", multiAssociation=" + multiAssociation +
//				", givenName='" + givenName + '\'' +
//				", firstName='" + firstName + '\'' +
//				", lastName='" + lastName + '\'' +
//				", middleName='" + middleName + '\'' +
//				", locale='" + locale + '\'' +
//				", city='" + city + '\'' +
//				", highestEducation=" + highestEducation +
//				", secondHighestEducation='" + secondHighestEducation + '\'' +
//				", gender=" + gender +
//				", ageRange=" + ageRange +
//				", chatUserName='" + chatUserName + '\'' +
//				", chatPassword='" + chatPassword + '\'' +
//				", createdDateTime=" + createdDateTime +
//				", lastUpdatedDateTime=" + lastUpdatedDateTime +
//				", timeZone='" + timeZone + '\'' +
//				", fkCountryId=" + (fkCountryId != null ? fkCountryId.toString() : null) +
//				'}';
//	}


	public User getUserFromRegistrationReq(RegistrationRequest req) {
    	this.setPrimaryEmail(req.getPrimaryEmail());
    	this.setIsPrimaryEmailPersonal(req.getIsPrimaryEmailPersonal());
    	this.setAlternateEmail(req.getAlternateEmail());
    	this.setIsAlternateEmailPersonal(req.getIsAlternateEmailPersonal());
    	this.setPersonalEmail(getPersonalEmail(req));
    	this.setCurrentOrgEmail(getCurrentOrgEmail(req));
    	this.setGivenName(req.getGivenName());
    	this.setFirstName(req.getFirstName());
    	this.setLastName(req.getLastName());
    	this.setMiddleName(req.getMiddleName());
		this.setImageData(req.getImageData());
//    	this.setLocale(req.getLocale());
//    	this.setCity(req.getCity());
//    	this.setHighestEducation(req.getHighestEducation());
//    	this.setSecondHighestEducation(req.getSecondHighestEducation());
//    	this.setGender(req.getGender());
//    	this.setAgeRange(req.getAgeRange());
		this.setFkCountryId(req.getCountry());
//    	this.setMultiAssociation(!req.getIsPrimaryEmailPersonal() && !req.getIsAlternateEmailPersonal());
		// ankit changes
		getMultiAssociation(req);
    	return this;
    }

	//  ankit code feb 14 2022
	public void getMultiAssociation(RegistrationRequest req) {
		if (req.getAlternateEmail() != null) {
			if (!req.getIsPrimaryEmailPersonal() && !req.getIsAlternateEmailPersonal()) {
				this.setMultiAssociation(true);
			} else {
				this.setMultiAssociation(false);
			}
		} else {
			this.setMultiAssociation(false);
		}
	}

	// utkarsh
//    private String getPersonalEmail(RegistrationRequest req) {
//    	String personalEmail;
//    	if (req.getIsPrimaryEmailPersonal()) {
//			personalEmail = req.getPrimaryEmail();
//		}else if (req.getIsAlternateEmailPersonal()) {
//			personalEmail = req.getAlternateEmail();
//		}else {
//			personalEmail = null;
//		}
//    	return personalEmail;
//    }

	//  ankit changes
	private String getPersonalEmail(RegistrationRequest req) {
		String personalEmail = null;
		if (req.getIsPrimaryEmailPersonal()) {
			personalEmail = req.getPrimaryEmail();
		}else if (req.getIsAlternateEmailPersonal() != null) {
			if (req.getIsAlternateEmailPersonal()) {
				personalEmail = req.getAlternateEmail();
			}
		}else {
			personalEmail = null;
		}
		return personalEmail;
	}


	private String getCurrentOrgEmail(RegistrationRequest req) {
    	String currentOrgEmail;
    	if (req.getIsPrimaryEmailPersonal()!=null && !req.getIsPrimaryEmailPersonal()) {
			currentOrgEmail = req.getPrimaryEmail();
		}else if (req.getIsAlternateEmailPersonal()!=null && !req.getIsAlternateEmailPersonal()) {
			currentOrgEmail = req.getAlternateEmail();
		}else {
			currentOrgEmail = null;
		}
    	return currentOrgEmail;
    }

	public String getRole() {
    	return "SAMPLE_HARDCODED";
	}

}
