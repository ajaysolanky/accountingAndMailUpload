//authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import controllers.routes;
import play.Logger;
import play.mvc.Http;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static controllers.Default.redirect;

@Entity
@Table(name = "upload_session")
public class MultiUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer id;
    @Column(name = "type")
    private String type;

    public MultiUpload(String type) {
        this.type = type;
    }

    public void saveToDB() {
        Ebean.save(this);
    }

    public Boolean doUploads(play.mvc.Http.Request req) {
        Http.MultipartFormData body  = req.body().asMultipartFormData();
        List<Http.MultipartFormData.FilePart> parts = body.getFiles();
        Http.MultipartFormData.FilePart fileUp;
        List<Upload> toBeProcessed = new ArrayList<Upload>();
        for (int i = 0; i < parts.size(); i++) {
            fileUp=parts.get(i);
            Upload upload = new Upload(id,type);
            if (upload.startUpload(fileUp)) {
                upload.finishUpload();
                toBeProcessed.add(upload);
            }
        }
        if (type.equals("accounting") || type.equals("template")) {
            for (Upload up : toBeProcessed) {
                up.startProcessing();
                up.finishProcessing();
            }
        }
        return true;
    }
}