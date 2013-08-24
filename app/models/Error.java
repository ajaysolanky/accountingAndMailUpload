package models;

import java.sql.Timestamp;
import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.annotation.CreatedTimestamp;

@Entity
@Table(name = "errors")
public class Error extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer id;

    @Column(name = "upload_id")
    public Integer uploadId;

    @Column(name = "upload_batch_attempt")
    public Integer uploadBatchId;

    @Column(name = "type")
    public String type;

    @Column(name = "file_name")
    public String fileName;

    @Column(name = "subtype")
    public String subtype;

    @Column(name = "message")
    public String message;

    @Column(name = "created_at")
    @CreatedTimestamp
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_visible")
    public Boolean isVisible = true;

    public Error(String fileName, Integer upId, Integer uploadBatchId, String typ, String subtyp, String mess) {
        this.fileName = fileName;
        this.uploadId = upId;
        this.uploadBatchId = uploadBatchId;
        this.type = typ;
        this.subtype = subtyp;
        this.message = mess;
    }

    public static List<Error> getErrors() {
        Finder<Long,Error> find = new Finder<Long,Error>(Long.class, Error.class);
        return find.all();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUploadId() {
        return uploadId;
    }

    public  String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public Boolean isVisible() {
        return isVisible;
    }

    @Override
    public void save() {
        if ((id == null)) {
            setCreatedAt();
            com.avaje.ebean.Ebean.save(this);
        } else {
            setUpdatedAt();
            com.avaje.ebean.Ebean.save(this);
        }
    }

    protected void setCreatedAt() {
        long time = System.currentTimeMillis();
        this.createdAt = new Timestamp(time);
    }

    protected void setUpdatedAt() {
        long time = System.currentTimeMillis();
        this.updatedAt = new Timestamp(time);
    }
}
