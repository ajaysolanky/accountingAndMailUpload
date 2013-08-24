//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package models;

import java.sql.Timestamp;

import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;
import play.*;
import play.db.ebean.Model;
import play.mvc.*;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import java.lang.Integer;
import com.avaje.ebean.Ebean;
import javax.persistence.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

@Entity
public class Upload extends Model {

    @Id
    protected Integer id;

    @Basic
    protected Integer uploadBatchId;
    protected String fileName;
    protected Timestamp uploadStarted;
    protected Timestamp uploadFinished;
    protected Timestamp processingStarted;
    protected Timestamp processingFinished;
    protected int recordCount;
    protected int errorCount;
    protected int skipCount;
    protected String type;
    protected boolean shouldDelete=false;

    public Upload(Integer id, String type){
        this.uploadBatchId = id;
        this.type = type;
    }

    public Boolean startUpload(FilePart fileUp) {
        uploadStarted = new Timestamp(new java.util.Date().getTime());
        fileName = fileUp.getFilename().substring(fileUp.getFilename().indexOf("/")+1);
        if (FileHelper.isFileInUploadFolder(fileName,type)|| FileHelper.isFileInProcessedFolder(fileName,type)) {
            SqlQuery query = Ebean.createSqlQuery("SELECT ID,UPLOAD_BATCH_ID FROM UPLOAD WHERE FILE_NAME = :FILENAME");
            query.setParameter("FILENAME",fileName);
            SqlRow row = query.findUnique();
            try {
                id = Integer.parseInt(row.get("id").toString());
            } catch (Exception e) {
                id=null;
            }
            new Error(fileName,id,uploadBatchId,"Duplicate File","...","Tried uploading file when it already exists in uploadedFolder or processedFolder").save();
            return false;
        }
        File file = fileUp.getFile();
        try {
            FileHelper.putInUploadFolder(file, fileName,type);
        } catch (IOException e) {
            new Error(fileName,id,uploadBatchId,"IOException","File Move Failed","Tried moving a file but it failed for some reason").save();
            return false;
        }
        return true;
        //Commented out code below is done with plain SQL, could be useful later
//        SqlUpdate update = Ebean.createSqlUpdate("INSERT INTO upload (file_name,upload_started,upload_finished,processing_started,processing_finished,record_count,error_count,skip_count,should_delete) VALUES ('Somefile.csv',NULL,NULL,NULL,NULL,0,0,0,0);");
//        Ebean.execute(update);
    }

    public void finishUpload() {
        uploadFinished = new Timestamp(new java.util.Date().getTime());
        Ebean.save(this);
    }

    public void startProcessing() {
        processingStarted = new Timestamp(new java.util.Date().getTime());
        if (type.equals("accounting")) {
            String path = FileHelper.getUploadedPath(getFileName(),type);
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(path));
            } catch (FileNotFoundException e1) {
                new Error(fileName,this.id,uploadBatchId,"FileNotFoundException",e1.toString(),"Error in Upload::startProcessing() while trying to get the file to upload").save();
                incrementErrorCount();
                return;
            }
            String currentLine="";
            Transaction trans;
            try {
                br.readLine();
            } catch (IOException e2) {
                new Error(fileName,this.id,uploadBatchId,"IOException",e2.toString(),"Error in Upload::startProcessing() while trying to skip the first line").save();
                incrementErrorCount();
            }
            int counter = 2;
            Ebean.beginTransaction();
            try{
                while(currentLine!=null) {
                    try{
                        incrementRecordCount();
                        currentLine = br.readLine();
                        if (currentLine==null) break;
                        trans = new Transaction(id,currentLine);
                        Ebean.save(trans);
                    } catch(IOException e3) {
                        new Error(fileName,this.id,uploadBatchId,"IOException",e3.toString(),"Error in Upload::startProcessing() at line "+counter+" while trying to read a line").save();
                        incrementErrorCount();
                        incrementSkipCount();
                    } catch(Exception e4) {
                        new Error(fileName,this.id,uploadBatchId,"Exception",e4.toString(),"Error in Upload::startProcessing() at line "+counter+" most likely while trying to save a Transaction").save();
                        incrementErrorCount();
                        incrementSkipCount();
                    } finally {
                        counter++;
                    }
                }
                Ebean.commitTransaction();
            } finally {
                Ebean.endTransaction();
            }
        }
    }

    public void finishProcessing() {
        try {
            FileHelper.moveFileFromUploadedToProcessed(getFileName(), type);
        } catch(Exception e) {
            new Error(fileName,this.id,uploadBatchId,"Exception",e.toString(),"Error in Upload::startProcessing() while trying to move the file from uploaded to processed").save();
            incrementErrorCount();
        }
        processingFinished = new Timestamp(new java.util.Date().getTime());
        SqlUpdate update = Ebean.createSqlUpdate("update upload set processing_started = '"+processingStarted.toString()+"', processing_finished = '"+processingFinished.toString()+"', record_count = '"+getRecordCount()+"', error_count = '"+getErrorCount()+"', skip_count = '"+getSkipCount()+"' where id = '"+id+"'");
        Ebean.execute(update);
    }

    public void incrementRecordCount() {
        recordCount++;
    }

    public void incrementSkipCount() {
        skipCount++;
    }

    public void incrementErrorCount() {
        errorCount++;
    }

    public void markForDeletion() {
        shouldDelete = true;
        SqlUpdate update = Ebean.createSqlUpdate("update upload set should_delete = '1' where id = '"+id+"'");
        Ebean.execute(update);
    }

    public boolean isUploading() {
        return (uploadStarted!=null && uploadFinished==null);
    }

    public boolean isProcessing() {
        if (processingStarted != null && processingFinished == null) return true;
        return false;
    }

    public boolean hasBeenProcessed() {
        if (processingFinished != null) return true;
        return false;
    }

    public void deleteUpload(){
        markForDeletion();
        if (!isUploading()&&!isProcessing()) {
            if (FileHelper.isFileInUploadFolder(getFileName(),type)) {
                try{
                    FileHelper.moveFileFromUploadedToProcessed(getFileName(),type);
                } catch (Exception e1) {
                    Error err = new Error(fileName,this.id,uploadBatchId,"Exception",e1.toString(),"Error in Upload::deleteUpload() while trying to move file from upload to processed");
                    err.save();
                    incrementErrorCount();
                }
            }
            if (FileHelper.isFileInProcessedFolder(getFileName(),type)) {
                try {
                    FileHelper.deleteFile(getFileName(),type);
                } catch (Exception e2) {
                    Error err = new Error(fileName,this.id,uploadBatchId,"Exception",e2.toString(),"Error in Upload::deleteUpload() while trying to delete file from processed");
                    err.save();
                    incrementErrorCount();
                }
            }
            else {
                Error err = new Error(fileName,this.id,uploadBatchId,"Warning","Missing File","The file should have been in processedFiles but it wasn't");
                err.save();
                incrementErrorCount();
            }
            try {
                Ebean.execute(Ebean.createSqlUpdate("delete from upload where id='"+id+"';"));
                Ebean.execute(Ebean.createSqlUpdate("delete from transaction where upload_id='"+id+"';"));
            } catch(Exception e3) {
                Error err = new Error(fileName,this.id,uploadBatchId,"Exception",e3.toString(),"Error in Upload::deleteUpload() while trying to delete the upload from the upload table");
                err.save();
                incrementErrorCount();
            }
        }
    }

    public boolean shouldBeDeleted() {
        return shouldDelete;
    }

    public String getType() {
        return type;
    }

    public static List<Upload> getUploads() {
        Model.Finder<Integer,Upload> find = new Finder<Integer,Upload>(Integer.class,Upload.class);
        return find.all();
    }

    public String getFileName() {
        return fileName;
    }

    public String getUploadedDate() {
        if (uploadFinished == null) return "Uploading...";
        return new java.util.Date(uploadFinished.getTime()).toString();
    }

    public String getProcessingTime() {
        if(processingFinished == null) return "Processing/Not Yet Processed";
        return String.valueOf(processingFinished.getTime()-processingStarted.getTime());
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public int getId() {
        return id;
    }

    public int getUploadBatchId() {
        return uploadBatchId;
    }
}
