//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import play.Logger;

import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class FileHelper {
    public static String uploadedPath;
    public static String processedPath;

    public static String getUploadedPath (String fileName, String type) {
        if(type.equals("template")) return "./public/templates/"+fileName;
        return "./public/uploadedFiles/"+type+"/"+fileName;
    }

    public static String getProcessedPath (String fileName, String type) {
        if(type.equals("template")) return "./public/templates/"+fileName;
        return "./public/processedFiles/"+type+"/"+fileName;
    }

    public static boolean isFileInUploadFolder (String fileName, String type) {
        File folder;
        if(type.equals("template")) folder = new File("./public/templates");
        else folder = new File("./public/uploadedFiles/"+type);
        File[] fileList = folder.listFiles();
        File currFile;
        for (int i = 0; i < fileList.length; i++) {
            currFile = fileList[i];
            if (currFile.getName().equals(fileName)){
                return true;
            }
        }
        return false;
    }

    public static boolean isFileInProcessedFolder (String fileName, String type) {
        File folder;
        if(type.equals("template")) folder = new File("./public/templates");
        else folder = new File("./public/processedFiles/"+type);
        File[] fileList = folder.listFiles();
        File currFile;
        for (int i = 0; i < fileList.length; i++) {
            currFile = fileList[i];
            if (currFile.getName().equals(fileName)) return true;
        }
        return false;
    }

    public static File getFileContents(String fileName, String type) {
        File file;
        if (isFileInUploadFolder(fileName, type)) file = new File(getUploadedPath(fileName,type));
        else if (isFileInProcessedFolder(fileName, type)) file = new File(getProcessedPath(fileName,type));
        else return null;
        return file.getAbsoluteFile();
    }

    public static void moveFileFromUploadedToProcessed (String fileName, String type) throws Exception {
        if (!isFileInUploadFolder(fileName, type)) {
            throw new Exception();
        }
        if (isFileInProcessedFolder(fileName, type)) {
            throw new Exception();
        }
        if(type.equals("template")) return;
        Path source = FileSystems.getDefault().getPath(getUploadedPath(fileName, type));
        Path newDir = FileSystems.getDefault().getPath("./public/processedFiles/"+type);
        Files.move(source,newDir.resolve(fileName));
    }

    public static void deleteFile (String fileName, String type) throws Exception {
        if (isFileInProcessedFolder(fileName, type)) {
            Files.delete(FileSystems.getDefault().getPath(getProcessedPath(fileName,type)));
        }
        else if (isFileInUploadFolder(fileName, type)) {
            Files.delete(FileSystems.getDefault().getPath(getUploadedPath(fileName,type)));
        }
        else throw new Exception();
    }

    public static String[] splitLine (String currentLine) {
        currentLine=currentLine.replaceAll(",,",", ,");
        currentLine=currentLine+" ";
        String[] split = currentLine.split(",");
        for (int i=0;i<split.length;i++){
            split[i]=split[i].replaceAll("//s+","");
        }
        return split;
    }

    public static String makeUploadRecords(int id) {
        SqlQuery query = Ebean.createSqlQuery("SELECT UPLOAD.FILE_NAME,UPLOAD.UPLOAD_STARTED,UPLOAD.UPLOAD_FINISHED,UPLOAD.PROCESSING_STARTED,UPLOAD.PROCESSING_FINISHED,UPLOAD.RECORD_COUNT,UPLOAD.ERROR_COUNT,UPLOAD.SKIP_COUNT,UPLOAD.SHOULD_DELETE,UPLOAD.ID,UPLOAD.TYPE FROM UPLOAD WHERE UPLOAD.UPLOAD_BATCH_ID = :BATCH");
        query.setParameter("BATCH",id);
        String result = "{\"uploads\": [";
        for(SqlRow row : query.findList()) {
            result+= "{\"File Name\": \""+row.get("FILE_NAME")+"\", \"Date Uploaded\": \""+row.get("UPLOAD_STARTED")+"\", \"Processing Time\": \""+FileHelper.getProcessingTime((Timestamp)row.get("PROCESSING_STARTED"), (Timestamp)row.get("PROCESSING_FINISHED"))+"\", \"Type\": \""+row.get("TYPE")+"\", \"Record Count\": \""+row.get("RECORD_COUNT")+"\", \"Error Count\" : \""+row.get("ERROR_COUNT")+"\", \"Skip Count\": \""+row.get("SKIP_COUNT")+"\", \"id\": \""+row.get("ID")+"\"},";
        }
        if(result.substring(result.length()-1).equals(",")){
            result = result.substring(0,result.length()-1);
        }
        result+="]}";
        return result;
    }

    public static String makeJsonErrors(int id) {
        SqlQuery query = Ebean.createSqlQuery("SELECT MESSAGE,FILE_NAME FROM ERRORS WHERE UPLOAD_BATCH_ATTEMPT = :BATCH");
        query.setParameter("BATCH",id);
        String jsonString = "{\"errors\":[";
        for (SqlRow row : query.findList()) {
            jsonString += "{\"message\":\""+row.get("message")+"\",\"file_name\":\""+row.get("file_name")+"\"},";
        }
        if(jsonString.substring(jsonString.length()-1).equals(",")){
            jsonString = jsonString.substring(0,jsonString.length()-1);
        }
        jsonString +="]}";
        return jsonString;
    }

    public static String stripQuotes(String s) {
        if(s==null) return s;
        if(s.equals("")) return s;
        if(s.substring(0,1).equals("'")) return s.substring(1);
        if (s.substring(0,1).equals("\"") && s.substring(s.length()-1).equals("\"")) return s.substring(1,s.length()-1);
        return s;
    }


    public static String getProcessingTime(Timestamp start, Timestamp finish) {
        if(finish == null) return "Processing/Not yet processed";
        return String.valueOf(finish.getTime()-start.getTime());
    }

    public static void putInUploadFolder(File file, String fileName, String type) throws IOException {
        Path source = FileSystems.getDefault().getPath(file.getPath());
        Path newDir;
        if (type.equals("template")) newDir = FileSystems.getDefault().getPath("./public/templates");
        else newDir = FileSystems.getDefault().getPath("./public/uploadedFiles/"+type);
        Files.move(source,newDir.resolve(fileName));
    }

    public static String stringifyHTML(String htmlFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("./public/templates/"+htmlFile));
        String currLine;
        String templateHTML = "";
        while((currLine = br.readLine()) != null) {
            templateHTML += currLine;
        }
        br.close();
        return templateHTML;
    }

    public static String getMailFileData () {
        File sendFolder = new File("./public/uploadedFiles/mail");
        File[] csvList = sendFolder.listFiles();
        File templateFolder = new File("./public/templates");
        File[] templateList = templateFolder.listFiles();
        String jstring="{\"csv\":[";
        File currFile;
        for (int i = 0; i < csvList.length; i++) {
            currFile = csvList[i];
            if (currFile.getName().equals(".DS_Store")) continue;
            jstring+="{\"file name\":\""+currFile.getName()+"\"}";
            if (i != csvList.length-1) jstring+=",";
        }
        jstring+="],\"templates\":[";
        for (int j = 0; j < templateList.length;j++) {
            currFile = templateList[j];
            if (currFile.getName().equals(".DS_Store")) continue;
            jstring+="{\"file name\":\""+currFile.getName()+"\"}";
            if (j != templateList.length-1) jstring+=",";
        }
        jstring+="]}";
        return jstring;
    }

    public static String[] splitLineMail (String currentLine) {
        String[] split = currentLine.split(",");
        if (split.length != 12) {
            ArrayList<String> list = new ArrayList<String>(Arrays.asList(split));
            String[] correctArray = new String[12];
            int iterator = 0;
            String concatLine;
            int j = 0;
            for (int i = 0; i < list.size(); i++) {
                if (j!=0) {
                    j--;
                    continue;
                }
                if (list.get(i).trim().substring(0,1).equals("\"") && !list.get(i).substring(list.get(i).trim().length()-1).equals("\"")) {
                    concatLine = list.get(i)+",";
                    j = i+1;
                    while(!list.get(j).substring(list.get(j).length()-1).equals("\"")) {
                        concatLine+=list.get(j)+",";
                        j++;
                    }
                    concatLine += list.get(j);
                    j-=i;
                    correctArray[iterator] = concatLine.replaceAll("\"","");
                    iterator++;
                }
                else {
                    correctArray[iterator] = list.get(i);
                    iterator++;
                }
            }
            split = correctArray;
        }
        return split;
    }

    public static String fixHTML(String html, String[] recipArray) {
        html = html.replaceAll("##INVOICENUMBER##",recipArray[0]);
        html = html.replaceAll("##NAME##",recipArray[1]);
        html = html.replaceAll("##STREETADDRESS##",recipArray[2]);
        html = html.replaceAll("##CITY##",recipArray[3]);
        html = html.replaceAll("##STATE##",recipArray[4]);
        html = html.replaceAll("##ZIPCODE##",recipArray[5]);
        html = html.replaceAll("##TRACKINGNUMBER##",recipArray[6]);
        html = html.replaceAll("##PRODUCT##",recipArray[7]);
        Calendar cal = Calendar.getInstance();
        html = html.replaceAll("##DATE##",(cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.YEAR));
        return html;
    }
}