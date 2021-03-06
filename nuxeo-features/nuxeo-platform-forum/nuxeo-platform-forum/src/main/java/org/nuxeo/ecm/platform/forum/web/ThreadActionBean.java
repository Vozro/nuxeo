/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     nulrich
 *
 * $Id
 */
package org.nuxeo.ecm.platform.forum.web;

import static org.jboss.seam.ScopeType.EVENT;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.web.CommentManagerActions;
import org.nuxeo.ecm.platform.comment.web.ThreadEntry;
import org.nuxeo.ecm.platform.forum.web.api.PostAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAdapter;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * This Action Listener represents a Thread inside a forum.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
@Name("threadAction")
@Scope(ScopeType.CONVERSATION)
public class ThreadActionBean implements ThreadAction {

    private static final Log log = LogFactory.getLog(ThreadActionBean.class);

    private static final long serialVersionUID = -2667460487440135732L;

    protected static final String schema = "thread";

    protected static final String type = "Thread";

    protected boolean principalIsAdmin;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient CommentManagerActions commentManagerActions;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected PostAction postAction;

    protected String title;

    protected String description;

    protected List<String> selectedModerators;

    protected boolean moderated;

    protected NuxeoPrincipal principal;

    public String addThread() {

        // The thread to be created
        DocumentModel docThread = getThreadModel();

        docThread = documentManager.createDocument(docThread);
        documentManager.save();

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
        clean();
        return navigationContext.navigateToDocument(docThread, "after-create");
    }

    /**
     * Clean variables.
     */
    protected void clean() {
        title = null;
        description = null;
        moderated = false;
        selectedModerators = null;
    }

    /**
     * Gets the Thread to create as a DocumentModel.
     */
    protected DocumentModel getThreadModel() {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String path = currentDocument.getPathAsString();

        final DocumentModel docThread = documentManager.createDocumentModel(type);
        docThread.setProperty("dublincore", "title", title);
        docThread.setProperty("dublincore", "description", description);
        docThread.setProperty(schema, "moderated", moderated);

        if (moderated) {

            List<String> selectedModerators = getSelectedModerators();

            // The current user should have the right to moderate
            if (!selectedModerators.contains(NuxeoPrincipal.PREFIX + currentUser.getName())) {
                selectedModerators.add(NuxeoPrincipal.PREFIX + currentUser.getName());
            }

            // XXX: hack, administrators should have the right to moderate
            // without being in this list
            // We automatically add administrators (with prefix) as moderators
            if (!selectedModerators.contains(NuxeoGroup.PREFIX + SecurityConstants.ADMINISTRATORS)) {
                selectedModerators.add(NuxeoGroup.PREFIX + SecurityConstants.ADMINISTRATORS);
            }
            // We can also remove Administrator since his group is added
            if (selectedModerators.contains(NuxeoPrincipal.PREFIX + SecurityConstants.ADMINISTRATOR)) {
                selectedModerators.remove(NuxeoPrincipal.PREFIX + SecurityConstants.ADMINISTRATOR);
            }

            docThread.setProperty(schema, "moderators", selectedModerators);
        }

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        docThread.setPathInfo(path, pss.generatePathSegment(docThread));
        return docThread;
    }

    @SuppressWarnings({ "unchecked" })
    public List<String> getModerators() {
        DocumentModel currentThread = navigationContext.getCurrentDocument();
        return (List<String>) currentThread.getProperty("thread", "moderators");
    }

    public boolean isPrincipalModerator() {
        principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        List<String> moderators = getModerators();

        boolean moderator = false;
        if (isPrincipalGroupModerator() || moderators != null
                && moderators.contains(NuxeoPrincipal.PREFIX + principal.getName())) {
            moderator = true;
        }
        return moderator;
    }

    public boolean isPrincipalGroupModerator() {

        List<String> moderators = getModerators();
        List<String> principalGroups = principal.getAllGroups();

        for (String principalGroup : principalGroups) {
            if (moderators != null && moderators.contains(NuxeoGroup.PREFIX + principalGroup)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentThreadModerated() {
        DocumentModel currentThread = navigationContext.getCurrentDocument();
        return isThreadModerated(currentThread);
    }

    @Factory(value = "currentThreadPosts", scope = EVENT)
    public List<ThreadEntry> getPostsAsThread() {
        List<ThreadEntry> basicCommentList = null;

        // Thread is not moderated, we return all Posts
        if (!isCurrentThreadModerated()) {
            basicCommentList = commentManagerActions.getCommentsAsThread();
        } else {
            // Here we clean the list according the rights of principal.
            basicCommentList = new ArrayList<ThreadEntry>();
            List<ThreadEntry> allThreadEntry = commentManagerActions.getCommentsAsThread();

            for (ThreadEntry threadEntry : allThreadEntry) {
                // if current user is not moderator and post is not published,
                // we remove it
                DocumentModel dm = threadEntry.getComment();

                String[] contributorsArray = (String[]) dm.getProperty("dublincore", "contributors");
                if (contributorsArray == null) {
                    contributorsArray = new String[0];
                }
                List<String> cs = Arrays.asList(contributorsArray);

                if (postAction.isPostPublished(threadEntry.getComment()) || isPrincipalModerator()
                        || cs.contains(currentUser.getName())) {
                    basicCommentList.add(threadEntry);
                }
            }
        }
        return basicCommentList;
    }

    protected ThreadAdapter adapter;

    public ThreadAdapter getAdapter(DocumentModel thread) {
        if (thread == null) {
            return null;
        }
        if (adapter != null && adapter.getThreadDoc().getRef().equals(thread.getRef())) {
            return adapter;
        }
        if (thread.getSessionId() == null) {
            try {
                thread = documentManager.getDocument(thread.getRef());
            } catch (DocumentNotFoundException e) {
                log.error("Unable to reconnect doc !,", e);
            }
        }
        adapter = thread.getAdapter(ThreadAdapter.class);
        return adapter;
    }

    public List<DocumentModel> getAllPosts(DocumentModel thread, String state) {

        thread = getDocumentThreadModel(thread.getRef());
        List<DocumentModel> allPosts = Collections.emptyList();
        List<ThreadEntry> allThreadEntry = Collections.emptyList();

        if (thread != null) {
            allThreadEntry = commentManagerActions.getCommentsAsThreadOnDoc(thread);
        }
        if (allThreadEntry != null && !allThreadEntry.isEmpty()) {
            allPosts = new ArrayList<DocumentModel>();
            for (ThreadEntry entry : allThreadEntry) {
                if (!"".equals(state) && state.equals(entry.getComment().getCurrentLifeCycleState())) {
                    allPosts.add(entry.getComment());
                } else if ("".equals(state)) {
                    allPosts.add(entry.getComment());
                }
            }

        }

        return allPosts;
    }

    public List<DocumentModel> getPostsPublished(DocumentModel thread) {
        return getAllPosts(thread, ForumConstants.PUBLISHED_STATE);
    }

    public List<DocumentModel> getPostsPending(DocumentModel thread) {
        return getAllPosts(thread, ForumConstants.PENDING_STATE);
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSchema() {
        return schema;
    }

    public String getType() {
        return type;
    }

    public void saveState() {
        log.info("PrePassivate");
    }

    public void readState() {
        log.info("PostActivate");
    }

    public boolean isModerated() {
        return moderated;
    }

    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }

    public DocumentModel getLastPostPublished(DocumentModel thread) {

        thread = getDocumentThreadModel(thread.getRef());
        List<DocumentModel> posts = getPostsPublished(thread);
        DocumentModel lastPost = null;
        if (!posts.isEmpty()) {
            lastPost = posts.get(0);
            for (DocumentModel post : posts) {
                GregorianCalendar lastPostDate = (GregorianCalendar) lastPost.getProperty("post", "creationDate");
                GregorianCalendar postDate = (GregorianCalendar) post.getProperty("post", "creationDate");
                if (postDate != null && postDate.after(lastPostDate)) {
                    lastPost = post;
                }
            }

        }
        return lastPost;
    }

    public String getModerationAsString(DocumentModel thread) {
        if (isThreadModerated(thread)) {
            return resourcesAccessor.getMessages().get("label.forum.thread.moderated.yes");
        }
        return resourcesAccessor.getMessages().get("label.forum.thread.moderated.no");
    }

    public boolean isThreadModerated(DocumentModel thread) {
        if (thread != null) {
            thread = getDocumentThreadModel(thread.getRef());
            if (thread != null) {
                Boolean moderation = (Boolean) thread.getProperty("thread", "moderated");
                if (moderation != null) {
                    return moderation;
                }
            }
        }
        return false;
    }

    public DocumentModel getParentPost(int post) {
        DocumentModel parentPost = null;

        List<ThreadEntry> posts = getPostsAsThread();
        if (post > 0 && post <= posts.size()) {
            ThreadEntry parent = posts.get(post - 1);
            ThreadEntry currentPost = posts.get(post);
            if (currentPost.getDepth() == parent.getDepth() + 1) {
                parentPost = parent.getComment();
            }
        }
        return parentPost;
    }

    public boolean isParentPostPublished(int post) {

        DocumentModel parent = getParentPost(post);
        if (parent == null) {
            return true;
        } else if (ForumConstants.PUBLISHED_STATE.equals(parent.getCurrentLifeCycleState())) {
            return true;
        }
        return false;
    }

    /**
     * Gets the thread for a given document reference.
     */
    protected DocumentModel getDocumentThreadModel(DocumentRef threadRef) {
        DocumentModel thread = null;
        if (threadRef != null) {
            thread = documentManager.getDocument(threadRef);
        }
        return thread;
    }

    public List<String> getSelectedModerators() {
        if (selectedModerators == null) {
            selectedModerators = new ArrayList<String>();
        }
        return selectedModerators;
    }

    public void setSelectedModerators(List<String> selectedModerators) {
        this.selectedModerators = selectedModerators;
    }

}
