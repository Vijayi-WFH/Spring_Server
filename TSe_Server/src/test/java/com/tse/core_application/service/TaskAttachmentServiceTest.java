package com.tse.core_application.service;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.FileMetadata;
import com.tse.core_application.custom.model.SearchCriteria;
import com.tse.core_application.custom.model.SearchOperation;
import com.tse.core_application.dto.DownloadAttachmentResponse;
import com.tse.core_application.dto.UploadAttachmentResponse;
import com.tse.core_application.exception.FileNameException;
import com.tse.core_application.exception.FileNotFoundException;
import com.tse.core_application.model.TaskAttachment;
import com.tse.core_application.repository.TaskAttachmentRepository;
import com.tse.core_application.service.Impl.TaskAttachmentService;
import com.tse.core_application.service.Impl.TaskServiceImpl;
import com.tse.core_application.specification.TaskAttachmentSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private TaskServiceImpl taskServiceImpl;

    @InjectMocks
    private TaskAttachmentService taskAttachmentService;


    /**
     * case - when all conditions are true and all files are inserted.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test
    public void testSaveFiles_Success() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", "file1.txt", null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.pdf", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();

        TaskAttachment taskAttachment1 = new TaskAttachment();
        taskAttachment1.setTaskId(100L);
        taskAttachment1.setFileName("file1.txt");
        taskAttachment1.setFileType("plain/text");
        taskAttachment1.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment1.setUploaderAccountId(1L);
        taskAttachment1.setFileSize(2000.0);
        taskAttachment1.setFileContent(fileContent);
        taskAttachment1.setRemoverAccountId(null);

        TaskAttachment taskAttachment2 = new TaskAttachment();
        taskAttachment2.setTaskId(100L);
        taskAttachment2.setFileName("file2.txt");
        taskAttachment2.setFileType("pdf");
        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment2.setUploaderAccountId(1L);
        taskAttachment2.setFileSize(2000.0);
        taskAttachment2.setFileContent(fileContent);
        taskAttachment2.setRemoverAccountId(null);

        attachmentsToSave.add(taskAttachment1);
        attachmentsToSave.add(taskAttachment2);

        ArrayList<TaskAttachment> attachmentsSaved = new ArrayList<>();

        TaskAttachment attachmentsSaved1 = new TaskAttachment();
        attachmentsSaved1.setTaskAttachmentId(1L);
        attachmentsSaved1.setTaskId(100L);
        attachmentsSaved1.setFileName("file1.txt");
        attachmentsSaved1.setFileType("plain/text");
        attachmentsSaved1.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved1.setUploaderAccountId(1L);
        attachmentsSaved1.setFileSize(2000.0);
        attachmentsSaved1.setFileContent(fileContent);
        attachmentsSaved1.setRemoverAccountId(null);

        TaskAttachment attachmentsSaved2 = new TaskAttachment();
        attachmentsSaved2.setTaskAttachmentId(2L);
        attachmentsSaved2.setTaskId(100L);
        attachmentsSaved2.setFileName("file2.txt");
        attachmentsSaved2.setFileType("pdf");
        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved2.setUploaderAccountId(1L);
        attachmentsSaved2.setFileSize(2000.0);
        attachmentsSaved2.setFileContent(fileContent);
        attachmentsSaved2.setRemoverAccountId(null);

        attachmentsSaved.add(attachmentsSaved1);
        attachmentsSaved.add(attachmentsSaved2);

        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);

        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = new ArrayList<>();
        for(TaskAttachment taskAttachment : attachmentsSaved) {
            UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse();
            attachmentResponse.setFileSize(taskAttachment.getFileSize());
            attachmentResponse.setFileFullName(taskAttachment.getFileName());
            savedDbAttachmentsResponse.add(attachmentResponse);
        }

        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        HashMap<String, Object> actual = taskAttachmentService.saveFiles(multipartFiles, 100L, 1L);
        assertThat(actual).isNotNull();
        assertThat(actual.get("success")).isNotNull();
    }

    /**
     * case - when all conditions are true and all files are inserted.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test
    public void testSaveFiles_Success_2() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", "file1.txt", null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.pdf", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();

        TaskAttachment taskAttachment1 = new TaskAttachment();
        taskAttachment1.setTaskId(100L);
        taskAttachment1.setFileName("file1.txt");
        taskAttachment1.setFileType("plain/text");
        taskAttachment1.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment1.setUploaderAccountId(1L);
        taskAttachment1.setFileSize(2000.0);
        taskAttachment1.setFileContent(fileContent);
        taskAttachment1.setRemoverAccountId(null);

        TaskAttachment taskAttachment2 = new TaskAttachment();
        taskAttachment2.setTaskId(100L);
        taskAttachment2.setFileName("file2.txt");
        taskAttachment2.setFileType("pdf");
        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment2.setUploaderAccountId(1L);
        taskAttachment2.setFileSize(2000.0);
        taskAttachment2.setFileContent(fileContent);
        taskAttachment2.setRemoverAccountId(null);

        attachmentsToSave.add(taskAttachment1);
        attachmentsToSave.add(taskAttachment2);

        List<TaskAttachment> attachmentsSaved = new ArrayList<>();

        TaskAttachment attachmentsSaved1 = new TaskAttachment();
        attachmentsSaved1.setTaskAttachmentId(1L);
        attachmentsSaved1.setTaskId(100L);
        attachmentsSaved1.setFileName("file1.txt");
        attachmentsSaved1.setFileType("plain/text");
        attachmentsSaved1.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved1.setUploaderAccountId(1L);
        attachmentsSaved1.setFileSize(2000.0);
        attachmentsSaved1.setFileContent(fileContent);
        attachmentsSaved1.setRemoverAccountId(null);

        TaskAttachment attachmentsSaved2 = new TaskAttachment();
        attachmentsSaved2.setTaskAttachmentId(2L);
        attachmentsSaved2.setTaskId(100L);
        attachmentsSaved2.setFileName("file2.txt");
        attachmentsSaved2.setFileType("pdf");
        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved2.setUploaderAccountId(1L);
        attachmentsSaved2.setFileSize(2000.0);
        attachmentsSaved2.setFileContent(fileContent);
        attachmentsSaved2.setRemoverAccountId(null);

        attachmentsSaved.add(attachmentsSaved1);
        attachmentsSaved.add(attachmentsSaved2);

        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);

        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = new ArrayList<>();
        for(TaskAttachment taskAttachment : attachmentsSaved) {
            UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse();
            attachmentResponse.setFileSize(taskAttachment.getFileSize());
            attachmentResponse.setFileFullName(taskAttachment.getFileName());
            savedDbAttachmentsResponse.add(attachmentResponse);
            assertTrue(savedDbAttachmentsResponse.contains(attachmentResponse));
        }

        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        HashMap<String, Object> actual = taskAttachmentService.saveFiles(multipartFiles, 100L, 1L);
        assertNotNull(actual);
        assertThat(actual.get("success")).isNotNull();
    }


    /**
     * case - when all conditions are true but IOException thrown.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test(expected = FileNameException.class)
    public void testSaveFiles_FileNameExceptionCase() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", "file1.txt", null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.pdf", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();

        TaskAttachment taskAttachment1 = new TaskAttachment();
        taskAttachment1.setTaskId(100L);
        taskAttachment1.setFileName("file1.txt");
        taskAttachment1.setFileType("plain/text");
        taskAttachment1.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment1.setUploaderAccountId(1L);
        taskAttachment1.setFileSize(2000.0);
        taskAttachment1.setFileContent(fileContent);
        taskAttachment1.setRemoverAccountId(null);

        TaskAttachment taskAttachment2 = new TaskAttachment();
        taskAttachment2.setTaskId(100L);
        taskAttachment2.setFileName("file2.txt");
        taskAttachment2.setFileType("pdf");
        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment2.setUploaderAccountId(1L);
        taskAttachment2.setFileSize(2000.0);
        taskAttachment2.setFileContent(fileContent);
        taskAttachment2.setRemoverAccountId(null);

        attachmentsToSave.add(taskAttachment1);
        attachmentsToSave.add(taskAttachment2);

        ArrayList<TaskAttachment> attachmentsSaved = new ArrayList<>();

        TaskAttachment attachmentsSaved1 = new TaskAttachment();
        attachmentsSaved1.setTaskAttachmentId(1L);
        attachmentsSaved1.setTaskId(100L);
        attachmentsSaved1.setFileName("file1.txt");
        attachmentsSaved1.setFileType("plain/text");
        attachmentsSaved1.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved1.setUploaderAccountId(1L);
        attachmentsSaved1.setFileSize(2000.0);
        attachmentsSaved1.setFileContent(fileContent);
        attachmentsSaved1.setRemoverAccountId(null);

        TaskAttachment attachmentsSaved2 = new TaskAttachment();
        attachmentsSaved2.setTaskAttachmentId(2L);
        attachmentsSaved2.setTaskId(100L);
        attachmentsSaved2.setFileName("file2.txt");
        attachmentsSaved2.setFileType("pdf");
        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved2.setUploaderAccountId(1L);
        attachmentsSaved2.setFileSize(2000.0);
        attachmentsSaved2.setFileContent(fileContent);
        attachmentsSaved2.setRemoverAccountId(null);

        attachmentsSaved.add(attachmentsSaved1);
        attachmentsSaved.add(attachmentsSaved2);

        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        throw new FileNameException();
    }

    /**
     * case - when some files cannot be inserted due to fileName validation error.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test
    public void testSaveFiles_ElseCase_If_2() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", "file1.abc", null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.pdf", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();

        UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse("file1.abc", 2000.0);
        taskAttachmentNotSavedDb.add(attachmentResponse);

        TaskAttachment taskAttachment2 = new TaskAttachment();
        taskAttachment2.setTaskId(100L);
        taskAttachment2.setFileName("file2.txt");
        taskAttachment2.setFileType("pdf");
        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
        taskAttachment2.setUploaderAccountId(1L);
        taskAttachment2.setFileSize(2000.0);
        taskAttachment2.setFileContent(fileContent);
        taskAttachment2.setRemoverAccountId(null);
        attachmentsToSave.add(taskAttachment2);

        ArrayList<TaskAttachment> attachmentsSaved = new ArrayList<>();

        TaskAttachment attachmentsSaved2 = new TaskAttachment();
        attachmentsSaved2.setTaskAttachmentId(2L);
        attachmentsSaved2.setTaskId(100L);
        attachmentsSaved2.setFileName("file2.txt");
        attachmentsSaved2.setFileType("pdf");
        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved2.setUploaderAccountId(1L);
        attachmentsSaved2.setFileSize(2000.0);
        attachmentsSaved2.setFileContent(fileContent);
        attachmentsSaved2.setRemoverAccountId(null);

        attachmentsSaved.add(attachmentsSaved2);

        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        HashMap<String, Object> actual = taskAttachmentService.saveFiles(multipartFiles, 100L, 1L);
        assertThat(actual.get("fail")).isNotNull();
    }

    /**
     * case - when some files cannot be inserted due to duplicate files validation error.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test
    public void testSaveFiles_ElseCase_If_3() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", "file1.txt", null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.pdf", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        allTaskAttachmentsFoundWithFileStatusA.add(new FileMetadata("file1.txt", 202.0, null));
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        List<String> allTaskAttachmentsNamesFoundWithFileStatusA = new ArrayList<>();
        allTaskAttachmentsNamesFoundWithFileStatusA.add("file1.txt");

//        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();

        UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse("file1.txt", 202.0);
        taskAttachmentNotSavedDb.add(attachmentResponse);

//        TaskAttachment taskAttachment2 = new TaskAttachment();
//        taskAttachment2.setTaskId(100L);
//        taskAttachment2.setFileName("file3.pdf");
//        taskAttachment2.setFileType("pdf");
//        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
//        taskAttachment2.setUploaderAccountId(1L);
//        taskAttachment2.setFileSize(2000.0);
//        taskAttachment2.setFileContent(fileContent);
//        taskAttachment2.setRemoverAccountId(null);
//        attachmentsToSave.add(taskAttachment2);

        ArrayList<TaskAttachment> attachmentsSaved = new ArrayList<>();

        TaskAttachment attachmentsSaved2 = new TaskAttachment();
        attachmentsSaved2.setTaskAttachmentId(2L);
        attachmentsSaved2.setTaskId(100L);
        attachmentsSaved2.setFileName("file2.pdf");
        attachmentsSaved2.setFileType("pdf");
        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved2.setUploaderAccountId(1L);
        attachmentsSaved2.setFileSize(2000.0);
        attachmentsSaved2.setFileContent(fileContent);
        attachmentsSaved2.setRemoverAccountId(null);

        attachmentsSaved.add(attachmentsSaved2);

//        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        HashMap<String, Object> actual = taskAttachmentService.saveFiles(multipartFiles, 100L, 1L);
        assertThat(actual.get("duplicate")).isNotNull();
    }

    /**
     * case - when some files cannot be inserted due to fileName extension validation error.
     * The method which is under test. @Link #{ saveFiles(List<MultipartFile> files, Long taskId, Long uploaderAccountId) throws IOException }.
     */
    @Test
    public void testSaveFiles_If_2() throws IOException {
        List<MultipartFile> multipartFiles = new ArrayList<>();
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart1 = new MockMultipartFile("file", null, null, fileContent);
        MockMultipartFile filePart2 = new MockMultipartFile("file", "file2.abc", null, fileContent);
        multipartFiles.add(filePart1);
        multipartFiles.add(filePart2);

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(1000L, Constants.FileAttachmentStatus.A)).thenReturn(allTaskAttachmentsFoundWithFileStatusA);

        ArrayList<TaskAttachment> attachmentsToSave = new ArrayList<>();
        ArrayList<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();

        UploadAttachmentResponse attachmentResponse1 = new UploadAttachmentResponse(null, 2000.0);
        UploadAttachmentResponse attachmentResponse2 = new UploadAttachmentResponse("file2.abc", 2000.0);

        taskAttachmentNotSavedDb.add(attachmentResponse1);
        taskAttachmentNotSavedDb.add(attachmentResponse2);

//        TaskAttachment taskAttachment2 = new TaskAttachment();
//        taskAttachment2.setTaskId(100L);
//        taskAttachment2.setFileName("file2.txt");
//        taskAttachment2.setFileType("pdf");
//        taskAttachment2.setFileStatus(Constants.FileAttachmentStatus.A);
//        taskAttachment2.setUploaderAccountId(1L);
//        taskAttachment2.setFileSize(2000.0);
//        taskAttachment2.setFileContent(fileContent);
//        taskAttachment2.setRemoverAccountId(null);
//        attachmentsToSave.add(taskAttachment2);
//
        ArrayList<TaskAttachment> attachmentsSaved = new ArrayList<>();
//
//        TaskAttachment attachmentsSaved2 = new TaskAttachment();
//        attachmentsSaved2.setTaskAttachmentId(2L);
//        attachmentsSaved2.setTaskId(100L);
//        attachmentsSaved2.setFileName("file2.txt");
//        attachmentsSaved2.setFileType("pdf");
//        attachmentsSaved2.setFileStatus(Constants.FileAttachmentStatus.A);
//        attachmentsSaved2.setUploaderAccountId(1L);
//        attachmentsSaved2.setFileSize(2000.0);
//        attachmentsSaved2.setFileContent(fileContent);
//        attachmentsSaved2.setRemoverAccountId(null);
//
//        attachmentsSaved.add(attachmentsSaved2);

        lenient().when(taskAttachmentRepository.saveAll(attachmentsToSave)).thenReturn(attachmentsSaved);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("attachments", 100L)).thenReturn(1);
        HashMap<String, Object> actual = taskAttachmentService.saveFiles(multipartFiles, 100L, 1L);
        assertThat(actual.get("fail").equals(taskAttachmentNotSavedDb));
    }


    /**
     * Case - when all conditions are true and the required active fileName is not found.
     * The method which is under test. @Link #{ getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long taskId, String fileName, Character fileStatus) }.
     */
    @Test(expected = FileNotFoundException.class)
    public void testGetTaskAttachmentByTaskIDAndFileNameAndFileStatus_FileNotFoundExceptionCase() {
        byte[] fileContent = "mockFile".getBytes(StandardCharsets.UTF_8);

        TaskAttachmentSpecification taSpecification = new TaskAttachmentSpecification();
        taSpecification.addSearchCriteria(new SearchCriteria("taskId", 100L, SearchOperation.EQUAL));
        taSpecification.addSearchCriteria(new SearchCriteria("fileName", "abc.txt", SearchOperation.EQUAL));
        taSpecification.addSearchCriteria(new SearchCriteria("fileStatus", 'A', SearchOperation.EQUAL));

        TaskAttachment attachmentsSaved1 = new TaskAttachment();
        attachmentsSaved1.setTaskAttachmentId(1L);
        attachmentsSaved1.setTaskId(100L);
        attachmentsSaved1.setFileName("file1.txt");
        attachmentsSaved1.setFileType("plain/text");
        attachmentsSaved1.setFileStatus(Constants.FileAttachmentStatus.A);
        attachmentsSaved1.setUploaderAccountId(1L);
        attachmentsSaved1.setFileSize(2000.0);
        attachmentsSaved1.setFileContent(fileContent);
        attachmentsSaved1.setRemoverAccountId(null);

        lenient().when(taskAttachmentRepository.findOne(taSpecification)).thenReturn(Optional.of(attachmentsSaved1));
        DownloadAttachmentResponse actual = taskAttachmentService.getTaskAttachmentByTaskIDAndFileNameAndFileStatus(100L, "file1.txt", 'A');

        assertThat(actual).isNotNull();
    }


    /**
     * Case - when all conditions are true and the single attachment of the task is deleted.
     * The method which is under test. @Link # { deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException }.
     */
    @Test
    public void testDeleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId_SingleAttachment_SuccessCase() throws IOException {

        List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = new ArrayList<>();
        allTaskAttachmentsFoundWithFileStatusA.add(new FileMetadata("abc.txt", 22.0, null));

        lenient().when(taskAttachmentRepository.updateTaskAttachmentStatusByTaskIAndFileName(100L, "abc.txt", 2L, 'D')).thenReturn(1);
        lenient().when(taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(100L, 'A')).thenReturn(allTaskAttachmentsFoundWithFileStatusA);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId("allTaskAttachmentsFoundWithFileStatusInString", 100L)).thenReturn(1);

        String actual = taskAttachmentService.deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(100L, "abc.txt", "single", 2L);
        assertThat(actual).containsIgnoringCase("success");
    }

    /**
     * Case - when all conditions are true and all the attachments of the task are deleted.
     * The method which is under test. @Link # { deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException }.
     */
    @Test
    public void testDeleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId_AllAttachments_SuccessCase() throws IOException {
        lenient().when(taskAttachmentRepository.updateAllTaskAttachmentsStatusByTaskIdWithoutComment(100L, 2L, 'D')).thenReturn(1);
        lenient().when(taskServiceImpl.updateTaskAttachmentsByTaskId(null, 100L)).thenReturn(1);

        String actual = taskAttachmentService.deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(100L, "", "all", 2L);
        assertThat(actual).containsIgnoringCase("success");
    }

    /**
     * Case - when fileNameException is thrown.
     * The method which is under test. @Link # { deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException }.
     */
    @Test(expected = FileNameException.class)
    public void testDeleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId_IfCase3_ElseCase() throws IOException {

        String actual = taskAttachmentService.deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(100L, "", "multiple", 2L);

    }

    /**
     * Case - when optionIndicator is invalid.
     * The method which is under test. @Link # { deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(Long taskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException }.
     */
    @Test
    public void testDeleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId_IfCase1_ElseCase() throws IOException {

        String actual = taskAttachmentService.deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(100L, "", null, 2L);
        assertThat(actual).containsIgnoringCase("Invalid Option Indicator");
    }



}
