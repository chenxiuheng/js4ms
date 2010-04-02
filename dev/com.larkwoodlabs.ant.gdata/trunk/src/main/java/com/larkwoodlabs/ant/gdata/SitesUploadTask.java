package com.larkwoodlabs.ant.gdata;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.Link;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.XhtmlTextConstruct;
import com.google.gdata.data.acl.AclEntry;
import com.google.gdata.data.acl.AclFeed;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.sites.ActivityFeed;
import com.google.gdata.data.sites.AnnouncementEntry;
import com.google.gdata.data.sites.AnnouncementsPageEntry;
import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseActivityEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ContentFeed;
import com.google.gdata.data.sites.FileCabinetPageEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.ListPageEntry;
import com.google.gdata.data.sites.RevisionFeed;
import com.google.gdata.data.sites.SiteEntry;
import com.google.gdata.data.sites.SiteFeed;
import com.google.gdata.data.sites.SitesLink;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.gdata.data.sites.WebPageEntry;
import com.google.gdata.data.spreadsheet.Column;
import com.google.gdata.data.spreadsheet.Field;
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
    boolean dumpSite = false;
    
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

    public void setDumpSiteContents(boolean dumpSite) {
        this.dumpSite = dumpSite;
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
        return "https://sites.google.com/feeds/content/" + this.domain + "/" + this.site + "/";
    }

    public void uploadAttachment() throws IOException, ServiceException  {
        if (this.dumpSite) listSiteContents("all");
        File file = new File(this.filename);
        String url = getContentFeedUrl();
        if (this.entryId.length() > 0) {
            url += this.entryId;
            AttachmentEntry entry = service.getEntry(new URL(url), AttachmentEntry.class);
            if (entry != null) {
                log("uploading "+this.filename+" as update to " + url);
                entry.setMediaSource(new MediaFileSource(file, this.mimeType));
                if (this.title.length() > 0) {
                    entry.setTitle(new PlainTextConstruct(this.title));
                }
                if (this.description.length() > 0) {
                    entry.setSummary(new PlainTextConstruct(this.description));
                }
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

    /**
     * Returns an entry's numeric ID.
     */
    private String getEntryId(BaseContentEntry<?> entry) {
      String selfLink = entry.getSelfLink().getHref();
      return selfLink.substring(selfLink.lastIndexOf("/") + 1);
    }

    /**
     * Returns an entry's numeric ID.
     */
    private String getEntryId(String selfLink) {
      return selfLink.substring(selfLink.lastIndexOf("/") + 1);
    }

    public String getRevisionFeedUrl() {
      return "https://sites.google.com/feeds/revision/" + this.domain + "/" + this.site + "/";
    }

    public String getActivityFeedUrl() {
      return "https://sites.google.com/feeds/activity/" + this.domain + "/" + this.site + "/";
    }

    public String getSiteFeedUrl() {
      return "https://sites.google.com/feeds/site/" + this.domain + "/";
    }
    
    public String getAclFeedUrl(String siteName) {
      return "https://sites.google.com/feeds/acl/site/" + this.domain + "/" + this.site + "/";
    }

    /**
     * Fetches and displays the user's site feed.
     */
    public void getSiteFeed() throws IOException, ServiceException {
      SiteFeed siteFeed = service.getFeed(
          new URL(getSiteFeedUrl()), SiteFeed.class);
      for (SiteEntry entry : siteFeed.getEntries()){
        System.out.println("title: " + entry.getTitle().getPlainText());
        System.out.println("site name: " + entry.getSiteName().getValue());
        System.out.println("theme: " + entry.getTheme().getValue());
        System.out.println("");
      }
    }

    /**
     * Fetches and displays a Site's acl feed.
     */
    public void getAclFeed(String siteName) throws IOException, ServiceException {
      AclFeed aclFeed = service.getFeed(
          new URL(getAclFeedUrl(siteName)), AclFeed.class);
      for (AclEntry entry : aclFeed.getEntries()) {
        System.out.println(entry.getScope().getValue() + " (" +
                           entry.getScope().getType() + ") : " + entry.getRole().getValue());
      }
    }

    /**
     * Fetches and displays the Site's activity feed.
     */
    public void getActivityFeed() throws IOException, ServiceException {
      ActivityFeed activityFeed = service.getFeed(
          new URL(getActivityFeedUrl()), ActivityFeed.class);
      for (BaseActivityEntry<?> entry : activityFeed.getEntries()){
        System.out.println(entry.getSummary().getPlainText());
      }
    }

    /**
     * Fetches and displays the revisions feed for an entry.
     */
    public void getRevisionFeed(String contentEntryId) throws IOException, ServiceException {
      URL url = new URL(getRevisionFeedUrl() + contentEntryId);
      RevisionFeed revisionFeed = service.getFeed(url, RevisionFeed.class);
      for (BaseContentEntry<?> entry : revisionFeed.getEntries()) {
        System.out.println(entry.getTitle().getPlainText());
        System.out.println("  updated: " + entry.getUpdated().toUiString() + " by " +
            entry.getAuthors().get(0).getEmail());
        System.out.println("  revision #: " + entry.getRevision().getValue());
      }
    }

    /**
     * Fetches and displays entries from the content feed.
     *
     * @param kind An entry kind to fetch. For example, "webpage". If null, the
     *     entire content feed is returned.
     */
    public void listSiteContents(String kind) throws IOException, ServiceException {
      String url = kind.equals("all") ? getContentFeedUrl() : getContentFeedUrl() + "?kind=" + kind;
      ContentFeed contentFeed = service.getFeed(new URL(url), ContentFeed.class);

      for (WebPageEntry entry : contentFeed.getEntries(WebPageEntry.class)) {
        System.out.println("WebPageEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        System.out.println("  authors: " + entry.getAuthors().get(0).getEmail());
        System.out.println("  content: " + getContentBlob(entry));
        System.out.println("");
      }

      for (ListPageEntry entry : contentFeed.getEntries(ListPageEntry.class)) {
        System.out.println("ListPageEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        for (Column col : entry.getData().getColumns()) {
          System.out.print("  [" + col.getIndex() + "] " + col.getName() + "\t");
        }
        System.out.println("");
      }

      for (ListItemEntry entry : contentFeed.getEntries(ListItemEntry.class)) {
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        for (Field field : entry.getFields()) {
          System.out.print("  [" + field.getIndex() + "] " + field.getValue() + "\t");
        }
        System.out.println("\n");
      }

      for (FileCabinetPageEntry entry : contentFeed.getEntries(FileCabinetPageEntry.class)) {
        System.out.println("FileCabinetPageEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        System.out.println("  content: " + getContentBlob(entry));
        System.out.println("");
      }

      for (CommentEntry entry : contentFeed.getEntries(CommentEntry.class)) {
        System.out.println("CommentEntry:");
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        System.out.println("  in-reply-to: " + entry.getInReplyTo().toString());
        System.out.println("  content: " + getContentBlob(entry));
        System.out.println("");
      }

      for (AnnouncementsPageEntry entry : contentFeed.getEntries(AnnouncementsPageEntry.class)) {
        System.out.println("AnnouncementsPageEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        System.out.println("  content: " + getContentBlob(entry));
        System.out.println("");
      }

      for (AnnouncementEntry entry : contentFeed.getEntries(AnnouncementEntry.class)) {
        System.out.println("AnnouncementEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        System.out.println("  draft?: " + entry.isDraft());
        System.out.println("  content: " + getContentBlob(entry));
        System.out.println("");
      }

      for (AttachmentEntry entry : contentFeed.getEntries(AttachmentEntry.class)) {
        System.out.println("AttachmentEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        if (entry.getSummary() != null) {
          System.out.println("  description: " + entry.getSummary().getPlainText());
        }
        System.out.println("  revision: " + entry.getRevision().getValue());
        MediaContent content = (MediaContent) entry.getContent();
        System.out.println("  src: " + content.getUri());
        System.out.println("  content type: " + content.getMimeType().getMediaType());
        System.out.println("");
      }

      for (WebAttachmentEntry entry : contentFeed.getEntries(WebAttachmentEntry.class)) {
        System.out.println("WebAttachmentEntry:");
        System.out.println("  title: " + entry.getTitle().getPlainText());
        System.out.println("  id: " + getEntryId(entry));
        if (entry.getParentLink() != null) {
          System.out.println("  parent id: " + getEntryId(entry.getParentLink().getHref()));
        }
        if (entry.getSummary() != null) {
          System.out.println("  description: " + entry.getSummary().getPlainText());
        }
        System.out.println("  src: " + ((MediaContent) entry.getContent()).getUri());
        System.out.println("");
      }
    }

    public String getContentBlob(BaseContentEntry<?> entry) {
     return ((XhtmlTextConstruct) entry.getTextContent().getContent()).getXhtml().getBlob();
    }
}
