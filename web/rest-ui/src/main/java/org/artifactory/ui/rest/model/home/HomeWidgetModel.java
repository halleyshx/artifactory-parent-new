package org.artifactory.ui.rest.model.home;

import com.google.common.collect.Maps;
import org.artifactory.rest.common.model.BaseModel;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Map;

/**
 * @author Dan Feldman
 */
public class HomeWidgetModel extends BaseModel {

    private String widgetName;
    private Map<Object, Object> widgetData = Maps.newHashMap();

    public HomeWidgetModel(String widgetName) {
        this.widgetName = widgetName;
    }

    @JsonIgnore
    public void addData(Object key, Object value) {
        widgetData.put(key, value);
    }

    public String getWidgetName() {
        return widgetName;
    }

    public Map getWidgetData() {
        return widgetData;
    }
}
