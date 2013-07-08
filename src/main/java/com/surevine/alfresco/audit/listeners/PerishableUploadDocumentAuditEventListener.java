/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in paragraph 1 bullet 4 of Alfrescos
 * standard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 *
 * This is free software: you can redistribute 
 * and/or modify it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * To see a copy of the GNU Lesser General Public License visit
 * <http://www.gnu.org/licenses/>.
 */
package com.surevine.alfresco.audit.listeners;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import com.surevine.alfresco.audit.AuditItem;
import com.surevine.alfresco.audit.Auditable;
import com.surevine.alfresco.repo.delete.PerishabilityLogic;

public class PerishableUploadDocumentAuditEventListener extends UploadDocumentAuditEventListener {

    /**
     * Logger for errors and warnings.
     */
    private static final Log logger = LogFactory.getLog(PerishableUploadDocumentAuditEventListener.class);
    
    private PerishabilityLogic _perishabilityLogic;
    
    public void setPerishabilityLogic(final PerishabilityLogic perishabilityLogic) {
        _perishabilityLogic = perishabilityLogic;
    }

    @Override
    protected void populateSecondaryAuditItems(List<Auditable> events, HttpServletRequest request,
            HttpServletResponse response, JSONObject postContent) throws JSONException {
        // Create a ServletFileUpload instance to parse the form
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        upload.setHeaderEncoding("UTF-8");
        
        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                String perishableReason = null;
                String tags = null;
            
                for (final Object o : upload.parseRequest(request)) {
                    FileItem item = (FileItem) o;
                    
                    if (item.isFormField() && "perishable".equals(item.getFieldName())) {
                        perishableReason = item.getString();
                    } else if (item.isFormField() && "tags".equals(item.getFieldName())) {
                        tags = item.getString();
                    }
                }
            
                Auditable primaryEvent = events.get(0);

                // If this is a site with perish reasons configured...
                if (_perishabilityLogic.getPerishReasons(primaryEvent.getSite()).size() > 0) {
                    AuditItem item = new AuditItem();
                    
                    setGenericAuditMetadata(item, request);
                    
                    // Copy any relevant fields from the primary audit event
                    item.setNodeRef(primaryEvent.getNodeRef());
                    item.setVersion(primaryEvent.getVersion());
                    item.setSecLabel(primaryEvent.getSecLabel());
                    item.setSource(primaryEvent.getSource());
                    
                    item.setAction(MarkPerishableAuditEventListener.ACTION);
                    
                    if(!StringUtils.isBlank(perishableReason)) {
                        item.setDetails(perishableReason);
                    } else {
                        item.setDetails(MarkPerishableAuditEventListener.NO_PERISHABLE_MARK);
                    }
                    
                    if (tags != null) {
                        item.setTags(StringUtils.join(tags.trim().split(" "), ','));
                    }
                    
                    events.add(item);
                }
            } catch (FileUploadException e) {
                logger.error("Error while parsing request form", e);
            }
        }
    }
}
