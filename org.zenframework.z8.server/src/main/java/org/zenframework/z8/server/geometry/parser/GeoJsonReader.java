package org.zenframework.z8.server.geometry.parser;

import java.util.ArrayList;
import java.util.List;

import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.json.parser.JsonObject;
import org.zenframework.z8.server.types.geometry;

public class GeoJsonReader {
	private final JsonObject json;
	private final int srs;

	static public geometry read(String json, int srs) {
		if(json == null || json.isEmpty())
			return new geometry(srs);

		return new GeoJsonReader(new JsonObject(json), srs).read();
	}

	private GeoJsonReader(JsonObject json, int srs) {
		this.json = json;
		this.srs = srs;
	}

	private geometry read() {
		return readGeometry(json);
	}

	private geometry readGeometry(JsonObject json) {
		String type = json.getString(GeoJson.Type);

		if(type.equals(GeoJson.Feature))
			return readFeature(json);
		else if(type.equals(GeoJson.FeatureCollection))
			return readFeatureCollection(json);
		else {
			int geoType = GeoJson.geometryType(type);
			JsonArray coordinates = json.getJsonArray(geoType != geometry.collection ? GeoJson.Coordinates : GeoJson.Geometries);
			return readGeometry(coordinates, geoType);
		}
	}

	private geometry readGeometry(JsonArray coordinates, int type) {
		switch(type) {
		case geometry.point:
			return readPoint(coordinates);
		case geometry.line:
			return readLine(coordinates);
		case geometry.polygon:
			return readPolygon(coordinates);
		case geometry.multiPoint:
			return readMultiPoint(coordinates);
		case geometry.multiLine:
			return readMultiLine(coordinates);
		case geometry.multiPolygon:
			return readMultiPolygon(coordinates);
		case geometry.collection:
			return readCollection(coordinates);
		default:
			return new geometry(srs);
		}
	}

	private geometry readPoint(JsonArray coordinates) {
		double x = coordinates.getDouble(0);
		double y = coordinates.getDouble(1);
		return new geometry(x, y, srs);
	}

	private List<geometry> readPoints(JsonArray points) {
		List<geometry> result = new ArrayList<geometry>();
		for(int i = 0; i < points.size(); i++)
			result.add(readPoint(points.getJsonArray(i)));
		return result;
	}

	private List<geometry> readCoordinates(JsonArray geometries, int type) {
		List<geometry> result = new ArrayList<geometry>();
		for(int i = 0; i < geometries.size(); i++)
			result.add(readGeometry(geometries.getJsonArray(i), type));
		return result;
	}

	private geometry readLine(JsonArray coordinates) {
		return new geometry(readPoints(coordinates), geometry.line, srs);
	}

	private geometry readRing(JsonArray coordinates) {
		return new geometry(readPoints(coordinates), geometry.ring, srs);
	}

	private geometry readPolygon(JsonArray coordinates) {
		List<geometry> rings = new ArrayList<geometry>();
		for(int i = 0; i < coordinates.size(); i++)
			rings.add(readRing(coordinates.getJsonArray(i)));
		return new geometry(rings, geometry.polygon, srs);
	}

	private geometry readMultiPoint(JsonArray coordinates) {
		return new geometry(readCoordinates(coordinates, geometry.point), geometry.multiPoint, srs);
	}

	private geometry readMultiLine(JsonArray coordinates) {
		return new geometry(readCoordinates(coordinates, geometry.line), geometry.multiLine, srs);
	}

	private geometry readMultiPolygon(JsonArray coordinates) {
		return new geometry(readCoordinates(coordinates, geometry.polygon), geometry.multiPolygon, srs);
	}

	private geometry readCollection(JsonArray geometries) {
		List<geometry> result = new ArrayList<geometry>();
		for(int i = 0; i < geometries.size(); i++)
			result.add(readGeometry(geometries.getJsonObject(i)));
		return new geometry(result, geometry.collection, srs);
	}

	private geometry readFeature(JsonObject json) {
		return readGeometry(json.getJsonObject(GeoJson.Geometry));
	}

	private geometry readFeatureCollection(JsonObject json) {
		List<geometry> geometries = new ArrayList<geometry>();
		JsonArray features = json.getJsonArray(GeoJson.Features);
		for(int i = 0; i < features.size(); i++)
			geometries.add(readFeature(features.getJsonObject(i)));
		return new geometry(geometries, geometry.collection, srs);
	}
}
