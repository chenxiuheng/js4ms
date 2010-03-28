package com.larkwoodlabs.ant.gdata;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.SitesLink;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class SitesUploadTask extends Task {

    static final String APPLICATION = "SitesUploadTask";

    String domain = "";
    String site = "";
    String entryId = "";
    String parentId = "";
    String username = "";
    String password = "";
    String filename = "";
    String mimeType = "application/octet-stream";
    String title = "";
    String description = "";
    
    SitesService service = null;
    MimetypesFileTypeMap mediaTypes;
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public void setSite(String site) {
        this.site = site;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void execute() {

        /*
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);

        // Configure the logging mechanisms
        Logger httpLogger = Logger.getLogger(HttpGDataRequest.class.getName());
        httpLogger.setLevel(Level.ALL);
        httpLogger.addHandler(logHandler);
        */
        
        this.service = new SitesService(APPLICATION);
        
        this.service.useSsl();
        try {
            login();
            uploadAttachment();
        }
        catch (AuthenticationException e) {
            throw new BuildException("authentication failed - " + e.getMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new BuildException("upload failed - " + e.getMessage());
        }
        catch (ServiceException e) {
            e.printStackTrace();
            throw new BuildException("upload failed - " + e.getMessage());
        }
    }

    void login() throws AuthenticationException {
        log("login("+this.username+")");
        service.setUserCredentials(this.username, this.password);
    }

    public String getContentFeedUrl() {
        return "http://sites.google.com/feeds/content/" + this.domain + "/" + this.site + "/";
    }

    public void uploadAttachment() throws IOException, ServiceException  {
        File file = new File(this.filename);
        String url = getContentFeedUrl();
        if (this.entryId.length() > 0) {
            url += this.entryId;
            AttachmentEntry entry = service.getEntry(new URL(url), AttachmentEntry.class);
            if (entry != null) {
                log("uploading "+this.filename+" as update to " + url);
                entry.setMediaSource(new MediaFileSource(file, this.mimeType));
                entry.updateMedia(true);
            }
            else {
                throw new BuildException("attachment identified as "+url+" does not exist");
            }
        }
        else {
            url += this.parentId;
            log("uploading "+this.filename+" as attachment to " + url);
            AttachmentEntry entry = new AttachmentEntry();
            entry.setMediaSource(new MediaFileSource(file, this.mimeType));
            entry.setTitle(new PlainTextConstruct(this.title));
            entry.setSummary(new PlainTextConstruct(this.description));
            entry.addLink(SitesLink.Rel.PARENT, Link.Type.ATOM, url);
            entry = service.insert(new URL(getContentFeedUrl()), entry);
        }
    }

}
