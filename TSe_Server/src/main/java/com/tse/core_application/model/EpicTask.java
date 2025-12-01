package com.tse.core_application.model;

import com.tse.core_application.utils.LongListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "epic_task", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EpicTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "epic_task_id")
    private Long epicTaskId;

    @ManyToOne
    @JoinColumn(name = "epic_id", referencedColumnName = "epic_id")
    private Epic fkEpicId;

    @ManyToOne
    @JoinColumn(name = "task_id", referencedColumnName = "task_id")
    private Task fkTaskId;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
