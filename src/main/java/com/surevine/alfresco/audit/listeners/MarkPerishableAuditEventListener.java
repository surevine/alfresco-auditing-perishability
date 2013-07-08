package com.surevine.alfresco.audit.listeners;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import com.surevine.alfresco.audit.AlfrescoJSONKeys;
import com.surevine.alfresco.audit.AuditItem;
import com.surevine.alfresco.audit.Auditable;
import com.surevine.alfresco.audit.BufferedHttpServletResponse;

public class MarkPerishableAuditEventListener extends PostAuditEventListener {

    /**
     * Part of the URI used to identify the event.
     */
    public static final String URI_DESIGNATOR = "sv-theme/delete/markPerishable";

    /**
     * Name of the event.
     */
    public static final String ACTION = "MARKED_PERISHABLE";
    
    public static final String NO_PERISHABLE_MARK = "NO PERISHABLE MARK APPLIED";

    private static final Log logger = LogFactory.getLog(MarkForDeleteAuditEventListener.class);

    /**
     * Default constructor which provides statics to the super class.
     */
    public MarkPerishableAuditEventListener() {
        super(URI_DESIGNATOR, ACTION, METHOD);
    }

    @Override
    public void setSpecificAuditMetadata(final Auditable auditable, final HttpServletRequest request,
            final JSONObject json, final BufferedHttpServletResponse response) throws JSONException {

        final String pathParam = request.getParameter("path");
        final String[] nodeRefs = request.getParameterValues(AlfrescoJSONKeys.NODEREF);
        
        if (pathParam != null) { // If the path parameter is non null then we resolve the nodeRef using the path.
            try {
                setMetadataFromNodeRef(auditable, nodeRefResolver.getNodeRefFromPath(pathParam));
            } catch (FileNotFoundException fne) {
                logger.warn("Could not find file for path: " + pathParam, fne);
            }
        } else if (nodeRefs.length == 0) {
            throw new RuntimeException("Expected either a path or nodeRef parameter. None found.");
        } else {
            setMetadataFromNodeRef(auditable, nodeRefResolver.getNodeRefFromGUID(nodeRefs[0]));
        }
        
        final String reason = json.getString("reason");
        
        if (StringUtils.isBlank(reason)) {
            auditable.setDetails(NO_PERISHABLE_MARK);
        } else {
            auditable.setDetails(reason);
        }
    }

    @Override
    protected void populateSecondaryAuditItems(List<Auditable> events, HttpServletRequest request,
            HttpServletResponse response, JSONObject postContent) throws JSONException {
        final String[] nodeRefs = request.getParameterValues(AlfrescoJSONKeys.NODEREF);
        
        if (request.getParameter("path") == null && nodeRefs.length > 1) {
            for (int i = 1; i<nodeRefs.length; i++) {
                Auditable primaryEvent = events.get(0);
                
                AuditItem item = new AuditItem();
                
                setGenericAuditMetadata(item, request);
                
                item.setAction(ACTION);
                
                setMetadataFromNodeRef(item, nodeRefResolver.getNodeRefFromGUID(nodeRefs[i]));
                item.setDetails(primaryEvent.getDetails());
                
                events.add(item);
            }
        }
    }

    @Override
    protected String getDetails(NodeRef nodeRef) {
        return super.getDetails(nodeRef);
    }
}
