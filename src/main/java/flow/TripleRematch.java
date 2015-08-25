package flow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;
import org.culturegraph.mf.types.Triple;

/**
 * @author philipp v. böselager based on
 *         org.culturegraph.mf.stream.pipe.sort,TripleSort by markus geipel
 */
@Description("Sorts triples and renames their keys if necessary")
@In(Triple.class)
@Out(Triple.class)
public final class TripleRematch extends AbstractTripleRematch {

	private String mRematchPredicate;
	private final Map<String, String> mRekeyPairs = new HashMap<>();

	/**
	 * @param aRematchPredicate a String defining which predicate of the processed
	 *          triples is used to check whether a triple has to be rematched
	 *          ("re-keyed") or not.
	 */
	public TripleRematch(String aRematchPredicate) {
		mRematchPredicate = aRematchPredicate;
	}

	@Override
	public void process(final Triple obj) {
		super.process(obj);
	}

	/**
	 * 
	 * @param aTripleSet a Set of Triples that have the same Predicate. The
	 *          Objects of these Triples might serve as an ID, but they should
	 *          not, if there is another Subject that owns this Object.
	 * @return a Map of String, String with the current Triple's Subject as a key
	 *         and the target Subject as a value.
	 */
	public static Map<String, String> findTriplesToBeRenamed(
			final Set<Triple> aTripleSet) {
		Map<String, String> triplesToBeRekeyed = new HashMap<>();
		for (Triple triple : aTripleSet) {
			if (triple.getSubject().equals(triple.getObject())) {
				for (Triple other : aTripleSet) {
					if (other.getObject().equals(triple.getObject())
							&& !(other.getSubject().equals(triple.getSubject()))) {
						triplesToBeRekeyed.put(triple.getSubject(), other.getSubject());
					}
				}
			}
		}
		return triplesToBeRekeyed;
	}

	@Override
	protected void identifyRekeyCases() {
		if (mRematchPredicate == null) {
			// nothing to rematch, return empty map
			return;
		}
		try {
			List<Triple> predicateTriples = getTriplesByPredicate(mRematchPredicate);
			for (Triple triple : predicateTriples) {
				if (triple.getSubject().equals(triple.getObject())) {
					// --> the object specified by mRematchPredicate has also been used as
					// subject for this record --> this is a potential re-key case --> see
					// if there is another triple containing this key as an object
					for (Triple sameObject : predicateTriples) {
						if (sameObject.getObject().equals(triple.getObject())
								&& !sameObject.getSubject().equals(triple.getSubject())) {
							// sameObject is part of the other record containing the same
							// object for the specified re-key predicate. --> the subject of
							// sameObject is the subject that triple should have
							mRekeyPairs.put(triple.getSubject(), sameObject.getSubject());
						}
					}
				}
			}
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	@Override
	protected void rematchedTriple(final Triple aTriple) {
		if (mRekeyPairs.containsKey(aTriple.getSubject())) {
			getReceiver().process(
					new Triple(mRekeyPairs.get(aTriple.getSubject()), aTriple
							.getPredicate(), aTriple.getObject()));
		} else {
			getReceiver().process(aTriple);

		}
	}
}
