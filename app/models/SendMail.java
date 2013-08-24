//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import play.Logger;
import play.db.ebean.Model;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SendMail {
    private static String api_user = "Julep Dev";
    private static String api_key = "JulepWeb2012";
    private String to;
    private String content;
    private String from;
    private String subj;

    public SendMail(String to, String content, String from, String subj) {
        this.to = to;
        this.content = content;
        this.from = from;
        this.subj = subj;
    }

    public static void sendAllFiles(String[][] csvsAndTemps) {
        SendMail curr;
        for (int i = 0; i < csvsAndTemps.length; i++) {
            curr = new SendMail(null,null,"shipping@julep.com","Julep Shipment Confirmation");

            SqlQuery query = Ebean.createSqlQuery("SELECT ID,UPLOAD_BATCH_ID FROM UPLOAD WHERE FILE_NAME = :FILENAME");
            query.setParameter("FILENAME",csvsAndTemps[i][0]);
            SqlRow row = query.findUnique();

            Upload up = new Model.Finder<Integer,Upload>(Integer.class,Upload.class).byId(Integer.parseInt(row.get("id").toString()));


            try{
                up.startProcessing();
                curr.readCSVAndSend(csvsAndTemps[i][0],csvsAndTemps[i][1],up.getId(),up.getUploadBatchId());
                up.finishProcessing();
            } catch (IOException e) {
                new Error(up.getFileName(),up.getId(),up.getUploadBatchId(),"IOException","...","Error mailing one of the csv files").save();
            }
        }
    }

    public void readCSVAndSend(String csv, String templateHtml, int id, int uploadBatchId) throws IOException {
        templateHtml = FileHelper.stringifyHTML(templateHtml);
        FileReader fReader = new FileReader("./public/uploadedFiles/mail/"+csv);
        BufferedReader recipients = new BufferedReader(fReader); //Open up the csv file
        int numberOfLines = 0;
        while(recipients.readLine() != null) numberOfLines++; //Count the number of lines
        recipients = new BufferedReader(new FileReader("./public/uploadedFiles/mail/"+csv)); //Reset the CSV file reader
        recipients.readLine(); //Skip the first line
        String[][] parsed = new String[numberOfLines-1][9];
        String currHTML;
        SendMail mail;
        String currentLine="";
        for (int i = 0; i < numberOfLines-1; i++) {
            try{
                currentLine = recipients.readLine();
                String[] split = FileHelper.splitLineMail(currentLine);
                //The big chunk of text below just breaks down currentLine with substring and stores the relevant info in parsed[][]
                parsed[i][1] = split[0]; //Name
                parsed[i][0] = split[1]; //Invoice Number
                parsed[i][7] = split[2]; //Product
                parsed[i][2] = split[4]; //Street Address
                parsed[i][3] = split[5]; //city
                parsed[i][4] = split[6]; //State
                parsed[i][5] = split[7]; //zip code
                parsed[i][8] = split[9]; //email
                parsed[i][6] = split[11]; //Tracking Number

                if (parsed[i][6].startsWith("'") && parsed[i][6].endsWith("'")) parsed[i][6] = parsed[i][6].substring(1,parsed[i][6].length()-1);

                //Now for the actual mail-sending
                currHTML = FileHelper.fixHTML(templateHtml, parsed[i]); //Replaces all the info in the HTML template file
                this.to = parsed[i][8];
                this.content = currHTML;
                System.out.println("Calling send to "+parsed[i][8]);
                System.out.println(send());
                System.out.println("Sent");
            }
            catch (Exception e) {
                new Error(csv,id,uploadBatchId,"Exception",e.getMessage(),"Line "+currentLine+" errored").save();
            }

        }
        fReader.close();
        recipients.close();
        System.out.println("CSV Completed.");
    }

    public String send() throws IOException{
        String request = "https://sendgrid.com/api/mail.send.json?api_user="+ URLEncoder.encode(api_user, "UTF-8")+"&api_key="+URLEncoder.encode(api_key,"UTF-8");
        String[] params = new String[4];
        params[0] = this.to;
        params[1] = this.from;
        params[2] = this.subj;
        params[3] = this.content;
        for (int i = 0; i < 4; i++) {
            params[i] = URLEncoder.encode(params[i],"UTF-8"); //put the parameters in a url-friendly format
        }
        String params_url = "&to="+params[0]+"&from="+params[1]+"&subject="+params[2]+"&html="+params[3]; //String together the parameters into the end of the url

        URL apiCall = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) apiCall.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST"); //Use post because the html file is very large

        DataOutputStream output = new DataOutputStream(conn.getOutputStream());
        output.writeBytes(params_url);
        if(output.size()==0) throw new IOException("DataOutputStream didn't write any bytes");
        output.flush();
        output.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String totalOutput = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null) totalOutput+=inputLine;
        return totalOutput;
    }
}