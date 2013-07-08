package com.surevine.alfresco.audit.listeners;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.surevine.alfresco.audit.AuditItem;
import com.surevine.alfresco.audit.Auditable;
import com.surevine.alfresco.repo.delete.PerishabilityLogic;

public class PerishableCreateDiscussionAuditEventListener extends
		CreateDiscussionAuditEventListener {

	private PerishabilityLogic _perishabilityLogic;

	public void setPerishabilityLogic(
			final PerishabilityLogic perishabilityLogic) {
		_perishabilityLogic = perishabilityLogic;
	}

	@Override
	protected void populateSecondaryAuditItems(List<Auditable> events,
			HttpServletRequest request, HttpServletResponse response,
			JSONObject postContent) throws JSONException {
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
			item.setSite(primaryEvent.getSite());

			item.setAction(MarkPerishableAuditEventListener.ACTION);

			final String perishableReason = postContent.has("perishable") ? postContent
					.getString("perishable") : null;

			if (!StringUtils.isBlank(perishableReason)) {
				item.setDetails(perishableReason);
			} else {
				item.setDetails(MarkPerishableAuditEventListener.NO_PERISHABLE_MARK);
			}

			events.add(item);
		}
	}
}
