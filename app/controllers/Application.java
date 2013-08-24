//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package controllers;

import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.Transaction;
import com.ning.http.client.filter.IOExceptionFilter;
import models.*;
import play.*;
import play.data.Form;
import play.db.ebean.Model;
import play.libs.Akka;
import play.libs.F;
import play.mvc.*;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import java.io.*;
import java.nio.file.*;
import com.avaje.ebean.*;
import play.db.*;
import scala.collection.immutable.List;
import views.html.*;

import java.util.Arrays;
import java.util.Date;

import static controllers.Default.redirect;
import static play.api.libs.json.Json.toJson;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.Callable;

public class Application extends Controller {
  
    public static Result index() {
        if (shouldHaveAccess()) {
            return ok(views.html.index.render());
        }
        Logger.debug("Access denied in index");
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result accountingHomeView() {
        if (shouldHaveAccess()) {
            return ok(views.html.accountingHome.render());
        }
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result viewUploads () {
        if (shouldHaveAccess()) {
//            Logger.debug("TYPE: "+request().queryString().keySet().contains("mail"));
            java.util.List<Upload> uploads = Upload.getUploads();
            return ok(views.html.uploads.render(uploads,request().getQueryString("type")));
        }
        Logger.debug("Access denied in viewUploadsAction");
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result viewErrorsAction() {
        if (shouldHaveAccess()){
            java.util.List<models.Error> errors = models.Error.getErrors();
            return ok(views.html.errorView.render(errors));
        }
        Logger.debug("Access denied in viewErrors");
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result uploadFileAction () {
        if (shouldHaveAccess()) {
            final Http.Request R = request();
//            Logger.debug("UPLOAD TYPE:"+ (R.body().asMultipartFormData().asFormUrlEncoded().get("type")[0].equals("accounting")));
            final MultiUpload UPLOADS = new MultiUpload(R.body().asMultipartFormData().asFormUrlEncoded().get("type")[0]);
            UPLOADS.saveToDB();
            Akka.future(
                    new Callable<Boolean>() {
                        public Boolean call() {
                            return UPLOADS.doUploads(R);
                        }
                    }
            );
            return ok(UPLOADS.id.toString());
        }
        Logger.debug("Access denied in uploadFileAction\n"+Thread.currentThread().getStackTrace());
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result deleteUploadAction () {
        if (shouldHaveAccess()) {
            Integer uploadId = Integer.parseInt(Form.form().bindFromRequest().get("uploadId"));
            Upload upload = Ebean.find(Upload.class,uploadId);
            upload.deleteUpload();
            return ok();
        }
        Logger.debug("Access denied in deleteUpload");
        return redirect(routes.Application.showAccessDenied());
    }

    public static boolean shouldHaveAccess() {
        return true;
    }

    public static Result reset () throws SQLException {
        if (shouldHaveAccess()) {
            Transaction tran = Ebean.beginTransaction();
            try {
                Connection conn = tran.getConnection();
                String sql1 = "DELETE FROM transaction;";
                String sql2 = "DELETE FROM upload;";
                String sql3 = "DELETE FROM errors";
                String sql4 = "DELETE FROM upload_session";
                conn.createStatement().executeUpdate(sql1);
                conn.createStatement().executeUpdate(sql2);
                conn.createStatement().executeUpdate(sql3);
                conn.createStatement().executeUpdate(sql4);

                File uploadedAcc = new File("./public/uploadedFiles/accounting");
                File processedAcc = new File("./public/processedFiles/accounting");
                File uploadedMail = new File("./public/uploadedFiles/mail");
                File processedMail = new File("./public/processedFiles/mail");
                File templates = new File("./public/templates");
                File[] files = uploadedAcc.listFiles();
                if (files!=null && files.length>0) for (File f : files) f.delete();
                files = processedAcc.listFiles();
                if (files!=null && files.length>0) for (File f : files) f.delete();
                files = uploadedMail.listFiles();
                if (files!=null && files.length>0) for (File f : files) f.delete();
                files = processedMail.listFiles();
                if (files!=null && files.length>0) for (File f : files) f.delete();
                files = templates.listFiles();
                if (files!=null && files.length>0) for (File f : files) f.delete();

                Ebean.commitTransaction();
            } finally {
                Ebean.endTransaction();
            }
            Ebean.getServerCacheManager().clearAll();

            return ok("Page has been reset");
        }
        Logger.debug("Access denied in reset");
        return redirect(routes.Application.showAccessDenied());
    }

//    Returns all the info for the upload table in JSON format
    public static Result jsonUploadTable() {
        Integer batch = Integer.parseInt(request().queryString().get("batch")[0]);
        return ok(FileHelper.makeUploadRecords(batch));
    }

    public static Result jsonErrorTable() {
        Integer batch = Integer.parseInt(request().queryString().get("batch")[0]);
        return ok(FileHelper.makeJsonErrors(batch));
    }

    //Mail functions below

    public static Result mail() {
        if (shouldHaveAccess()) {
            return ok(views.html.mail.render());
        }
        else return redirect(routes.Application.showAccessDenied());
    }

    public static Result sendMail() {
        if (shouldHaveAccess()) {
            return ok(views.html.mailForm.render(FileHelper.getMailFileData()));
        }
        else return redirect(routes.Application.showAccessDenied());
    }

    public static Result processMailFiles() {
        final Http.Request R = request();

        Akka.future(
                new Callable<Boolean>() {
                    public Boolean call() {
                        int arrLength = R.body().asFormUrlEncoded().size()/2;
                        String[][] data = new String[arrLength][2];
                        for (int i = 0; i < arrLength; i++) {
                            data[i][0] = R.body().asFormUrlEncoded().get("seanp["+i+"][file]")[0];
                            data[i][1] = R.body().asFormUrlEncoded().get("seanp["+i+"][template]")[0];
                        }
                        SendMail.sendAllFiles(data);
                        return true;
                    }
                }
        );

        return ok("complete");
    }

    public static Result showAccessDenied() {
        return redirect(routes.Application.showAccessDenied());
    }

    public static Result viewTemplates() {
        if (shouldHaveAccess()) {
            return ok(views.html.templates.render(Upload.getUploads()));
        }
        else return redirect(routes.Application.showAccessDenied());
    }
}