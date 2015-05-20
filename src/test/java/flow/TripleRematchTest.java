package flow;

import java.util.ArrayList;
import java.util.List;

import org.culturegraph.mf.types.Triple;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TripleRematchTest {

	private static List<Triple> mInputTriples = new ArrayList<>();
	private static List<Triple> mOutputTriples = new ArrayList<>();
	private static TripleRematch mTripleRematch = new TripleRematch("idB");

	@BeforeClass
	public static void setupTestData() {

		// Two big data sources are simulated: "A" and "B"

		// RECORD 1
		// - data source A
		mInputTriples.add(new Triple("rec01", "_id", "rec01"));
		mInputTriples.add(new Triple("rec01", "idA", "rec01"));
		mInputTriples.add(new Triple("rec01", "idB", "b_ID_001"));
		mInputTriples.add(new Triple("rec01", "infoFromA", "sunshine"));
		// - data source B
		mInputTriples.add(new Triple("rec01", "_id", "rec01"));
		mInputTriples.add(new Triple("rec01", "idA", "rec01"));
		mInputTriples.add(new Triple("rec01", "idB", "b_ID_001"));
		mInputTriples.add(new Triple("rec01", "infoFromB", "rain"));

		// RECORD 2
		// - data source A
		mInputTriples.add(new Triple("rec02", "_id", "rec02"));
		mInputTriples.add(new Triple("rec02", "idA", "rec02"));
		mInputTriples.add(new Triple("rec02", "idB", "b_ID_002"));
		mInputTriples.add(new Triple("rec02", "infoFromA", "shirt"));
		// - data source B (missing "idA" ! --> chooses "iDB" as "_id")
		mInputTriples.add(new Triple("b_ID_002", "_id", "b_ID_002"));
		mInputTriples.add(new Triple("b_ID_002", "idB", "b_ID_002"));
		mInputTriples.add(new Triple("b_ID_002", "infoFromB", "jeans"));

		mTripleRematch.setReceiver(new LocalReceiver());
	}

	@Test
	public void rematchTriples() {
		// process all triples
		for (Triple t : mInputTriples) {
			mTripleRematch.process(t);
		}
		mTripleRematch.onCloseStream();

		// assert that the number of triples has maintained (no doubles deletion in
		// this module)
		Assert.assertEquals(15, mOutputTriples.size());
		// assert that the first 12 triples have remained the same
		for (int i = 0; i < 12; i++) {
			Assert.assertEquals(mInputTriples.get(i), mOutputTriples.get(i));
		}
		// assert that the module has changed the subject of the triples of RECORD
		// 2, data source B
		Assert.assertTrue(mOutputTriples.get(12).equals(
				new Triple("rec02", "_id", "b_ID_002")));
		Assert.assertTrue(mOutputTriples.get(13).equals(
				new Triple("rec02", "idB", "b_ID_002")));
		Assert.assertTrue(mOutputTriples.get(14).equals(
				new Triple("rec02", "infoFromB", "jeans")));
	}

	private static class LocalReceiver extends AbstractTripleRematch {

		@Override
		public void process(final Triple namedValue) {
			mOutputTriples.add(namedValue);
		}

		@Override
		protected void identifyRekeyCases() {
			// not needed here
		}

		@Override
		protected void rematchedTriple(Triple aTriple) {
			// not needed here
		}

	}
}
