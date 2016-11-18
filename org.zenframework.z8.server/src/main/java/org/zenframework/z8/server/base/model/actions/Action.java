package org.zenframework.z8.server.base.model.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.request.RequestTarget;
import org.zenframework.z8.server.types.guid;
import org.zenframework.z8.server.types.string;

public abstract class Action extends RequestTarget {
	static public final String newAction = "new";
	static public final String createAction = "create";
	static public final String copyAction = "copy";
	static public final String readAction = "read";
	static public final String updateAction = "update";
	static public final String destroyAction = "destroy";
	static public final String moveAction = "move";
	static public final String reportAction = "report";
	static public final String previewAction = "preview";
	static public final String commandAction = "command";
	static public final String readRecordAction = "readRecord";
	static public final String modelAction = "model";
	static public final String attachAction = "attach";
	static public final String detachAction = "detach";

	private ActionParameters actionParameters;

	public Action(ActionParameters parameters) {
		super(parameters.requestId);

		this.actionParameters = parameters;
	}

	public ActionParameters actionParameters() {
		return actionParameters;
	}

	public Map<string, string> requestParameters() {
		return actionParameters.requestParameters();
	}

	public Query getQuery() {
		return actionParameters.query;
	}

	public Query getRootQuery() {
		Query query = getQuery();
		return query != null ? query.getRootQuery() : null;
	}

	public String getRequestParameter(string key) {
		return actionParameters.requestParameter(key);
	}

	public int getRequestParameter(string key, int defaultValue) {
		String value = getRequestParameter(key);
		return value != null && !value.isEmpty() ? Integer.parseInt(value) : defaultValue;
	}

	public guid getParentIdParameter() {
		String parentId = getRequestParameter(Json.parentId);
		return parentId == null ? null : parentId.isEmpty() ? guid.NULL : new guid(parentId);
	}

	public guid getRecordIdParameter() {
		String recordId = getRequestParameter(Json.recordId);
		return recordId != null ? new guid(recordId) : null;
	}

	public String getFieldParameter() {
		return getRequestParameter(Json.field);
	}

	public String getTypeParameter() {
		return getRequestParameter(Json.type);
	}

	public String getDetailsParameter() {
		return getRequestParameter(Json.details);
	}

	public guid getFilterByParameter() {
		String recordId = getRequestParameter(Json.filterBy);
		return recordId != null ? new guid(recordId) : null;
	}

	public guid getSourceParameter() {
		String recordId = getRequestParameter(Json.source);
		return recordId != null ? new guid(recordId) : null;
	}

	public int getTotalParameter() {
		String total = getRequestParameter(Json.total);
		return total != null ? Integer.parseInt(total) : -1;
	}

	public String getTextParameter() {
		return getRequestParameter(Json.text);
	}

	public String getGroupByParameter() {
		return getRequestParameter(Json.groupBy);
	}

	public String getGroupDirectionParameter() {
		return getRequestParameter(Json.groupDir);
	}

	public String getTotalsByParameter() {
		return getRequestParameter(Json.totalsBy);
	}

	public String getLookupParameter() {
		return getRequestParameter(Json.lookup);
	}

	public Collection<String> getLookupFields() {
		String lookupFields = getRequestParameter(Json.lookupFields);

		Collection<String> result = new ArrayList<String>();

		if(lookupFields == null)
			return result;

		JsonArray array = new JsonArray(lookupFields);

		for(int index = 0; index < array.length(); index++)
			result.add(array.getString(index));

		return result;
	}

	public String getDataParameter() {
		return getRequestParameter(Json.data);
	}

	public String getFormatParameter() {
		return getRequestParameter(Json.format);
	}

	public String getReportParameter() {
		return getRequestParameter(Json.report);
	}

	public String getOptionsParameter() {
		return getRequestParameter(Json.options);
	}

	public String getCommandParameter() {
		return getRequestParameter(Json.command);
	}

	public String getParametersParameter() {
		return getRequestParameter(Json.parameters);
	}

	public String getFilterParameter() {
		return getRequestParameter(Json.filter);
	}

	public String getQuickFilterParameter() {
		return getRequestParameter(Json.quickFilter);
	}

	public String getWhereParameter() {
		return getRequestParameter(Json.where);
	}

	public String getFilter1Parameter() {
		return getRequestParameter(Json.filter1);
	}

	public String getRecordParameter() {
		return getRequestParameter(Json.record);
	}

	public String getColumnsParameter() {
		return getRequestParameter(Json.columns);
	}

	public List<guid> getIdList() {
		List<guid> ids = new ArrayList<guid>();

		JsonArray records = new JsonArray(getDataParameter());

		for(int index = 0; index < records.length(); index++) {
			String id = records.getString(index);
			ids.add(new guid(id));
		}

		return ids;
	}
}
