/* Copyright 2016, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

@SuppressWarnings("javadoc")
public class CsvExportTest {

	@Test
	public void testFlatFields() {
		ObjectNode node1 = Json.newObject();
		node1.put("field1", "org1-value1");
		node1.put("field2", "org1-value2");
		node1.put("field3", "org1-value3");
		ObjectNode node2 = Json.newObject();
		node2.put("field1", "org2-value1");
		node2.put("field2", "org2-value2");
		node2.put("field3", "org2-value3");
		List<ObjectNode> orgs = Arrays.asList(node1, node2);
		CsvExport export = new CsvExport(Json.stringify(Json.toJson(orgs)));
		String expected = String.format("%s,%s\n%s,%s\n%s,%s\n", //
				"field1", "field3", //
				"\"org1-value1\"", "\"org1-value3\"", //
				"\"org2-value1\"", "\"org2-value3\"");
		assertThat(export.of(Arrays.asList("field1", "field3")))
				.isEqualTo(expected);
	}

	@Test
	public void testNestedFields() {
		ObjectNode org1 = Json.newObject();
		ObjectNode sub1 = Json.newObject();
		org1.put("field1", "org1-value1");
		org1.put("field2", "org1-value2");
		org1.set("field3", sub1);
		sub1.put("field1", "org1-sub1");
		sub1.put("field2", "org1-sub2");
		sub1.put("field3", "org1-sub3");
		ObjectNode org2 = Json.newObject();
		ObjectNode sub2 = Json.newObject();
		org2.put("field1", "org2-value1");
		org2.put("field2", "org2-value2");
		org2.set("field3", sub2);
		sub2.put("field1", "org2-sub1");
		sub2.put("field2", "org2-sub2");
		sub2.put("field3", "org2-sub3");
		List<ObjectNode> orgs = Arrays.asList(org1, org2);
		CsvExport export = new CsvExport(Json.stringify(Json.toJson(orgs)));
		String expected = String.format("%s,%s\n%s,%s\n%s,%s\n", //
				"field1", "field3.field2", //
				"\"org1-value1\"", "\"org1-sub2\"", //
				"\"org2-value1\"", "\"org2-sub2\"");
		assertThat(export.of(Arrays.asList("field1", "field3.field2")))
				.isEqualTo(expected);
	}

	@Test
	public void testMissingField() {
		ObjectNode org = Json.newObject();
		org.put("field1", "org1-value1");
		org.put("field2", "org1-value2");
		org.put("field3", "org1-value3");
		CsvExport export =
				new CsvExport(Json.stringify(Json.toJson(Arrays.asList(org))));
		String expected = String.format("%s,%s\n%s,%s\n", //
				"field1", "no-such-field", //
				"\"org1-value1\"", "");
		assertThat(export.of(Arrays.asList("field1", "no-such-field")))
				.isEqualTo(expected);
	}
}
