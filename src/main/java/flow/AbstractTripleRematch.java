/*
 *  Copyright 2013, 2014 Deutsche Nationalbibliothek
 *
 *  Licensed under the Apache License, Version 2.0 the "License";
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package flow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.ObjectReceiver;
import org.culturegraph.mf.stream.pipe.sort.SortedTripleFileFacade;
import org.culturegraph.mf.types.Triple;
import org.culturegraph.mf.util.MemoryWarningSystem;
import org.culturegraph.mf.util.MemoryWarningSystem.Listener;

/**
 * @author philipp v. böselager; based on
 *         org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort by markus
 *         geipel
 */
public abstract class AbstractTripleRematch extends
		DefaultObjectPipe<Triple, ObjectReceiver<Triple>> implements Listener {

	private final List<Triple> buffer = new ArrayList<>();
	private final List<File> tempFiles;
	private volatile boolean memoryLow;

	/**
	 * 
	 */
	public AbstractTripleRematch() {
		MemoryWarningSystem.addListener(this);
		tempFiles = new ArrayList<>();
		// Initialized here to let the compiler enforce the call to super() in
		// subclasses.
	}

	@Override
	public final void memoryLow(final long usedMemory, final long maxMemory) {
		memoryLow = true;
	}

	@Override
	public void process(final Triple namedValue) {
		if (memoryLow) {
			try {
				if (!buffer.isEmpty()) {
					nextBatch();
				}
			} catch (final IOException e) {
				throw new MetafactureException(
						"Error writing to temp file after sorting", e);
			} finally {
				memoryLow = false;
			}
		}
		buffer.add(namedValue);
	}

	private void nextBatch() throws IOException {
		if (!buffer.isEmpty()) {
			final File tempFile = File.createTempFile("sort", "namedValues", null);
			tempFile.deleteOnExit();
			try (final ObjectOutputStream out =
					new ObjectOutputStream(new FileOutputStream(tempFile))) {
				try {
					for (final Triple triple : buffer) {
						triple.write(out);
					}
				} finally {
					out.close();
				}
				buffer.clear();
				tempFiles.add(tempFile);
			}
		}
	}

	@Override
	public final void onCloseStream() {
		identifyRekeyCases();
		List<SortedTripleFileFacade> queue = null;
		try {
			nextBatch();
			queue = getTempFilesAsFacadeList();
			for (SortedTripleFileFacade sortedFileFacade : queue) {
				Triple triple = sortedFileFacade.pop();
				while (triple != null) {
					rematchedTriple(triple);
					triple = sortedFileFacade.pop();
				}
			}
			onFinished();
		} catch (final IOException e) {
			throw new MetafactureException("Error merging temp files", e);
		} finally {
			if (queue != null) {
				for (final SortedTripleFileFacade sortedFileFacade : queue) {
					sortedFileFacade.close();
				}
			}
		}
		MemoryWarningSystem.removeListener(this);
	}

	/**
	 */
	protected abstract void identifyRekeyCases();

	/**
	 */
	protected void onFinished() {
		// nothing to do
	}

	@Override
	public final void onResetStream() {
		buffer.clear();
		for (final File file : tempFiles) {
			if (file.exists()) {
				file.delete();
			}
		}
		tempFiles.clear();
	}

	/**
	 * @param aSubject defines the subject of all matching triples are to be
	 *          returned.
	 * @return an ArrayList of triples matching
	 * @throws IOException if
	 */
	protected List<Triple> getTriplesBySubject(String aSubject)
			throws IOException {
		List<Triple> matchingTriples = new ArrayList<>();
		nextBatch();
		List<SortedTripleFileFacade> queue = getTempFilesAsFacadeList();
		for (SortedTripleFileFacade sortedFileFacade : queue) {
			Triple triple = sortedFileFacade.pop();
			while (triple != null) {
				if (triple.getSubject().equals(aSubject)) {
					matchingTriples.add(triple);
				}
				triple = sortedFileFacade.pop();
			}
		}
		return matchingTriples;
	}

	/**
	 * @param aPredicate defines the predicate of all matching triples are to be
	 *          returned.
	 * @return an ArrayList of triples matching
	 * @throws IOException if
	 */
	protected List<Triple> getTriplesByPredicate(String aPredicate)
			throws IOException {
		List<Triple> matchingTriples = new ArrayList<>();
		nextBatch();
		final List<SortedTripleFileFacade> queue = getTempFilesAsFacadeList();
		for (SortedTripleFileFacade sortedFileFacade : queue) {
			Triple triple = sortedFileFacade.pop();
			while (triple != null) {
				if (triple.getPredicate().equals(aPredicate)) {
					matchingTriples.add(triple);
				}
				triple = sortedFileFacade.pop();
			}
		}
		return matchingTriples;
	}

	/**
	 * @param aObject defines the object of all matching triples are to be
	 *          returned.
	 * @return an ArrayList of triples matching
	 * @throws IOException if
	 */
	protected List<Triple> getTriplesByObject(String aObject) throws IOException {
		List<Triple> matchingTriples = new ArrayList<>();
		nextBatch();
		List<SortedTripleFileFacade> queue = getTempFilesAsFacadeList();
		for (SortedTripleFileFacade sortedFileFacade : queue) {
			Triple triple = sortedFileFacade.pop();
			while (triple != null) {
				if (triple.getObject().equals(aObject)) {
					matchingTriples.add(triple);
				}
				triple = sortedFileFacade.pop();
			}
		}
		return matchingTriples;
	}

	private List<SortedTripleFileFacade> getTempFilesAsFacadeList()
			throws IOException {
		List<SortedTripleFileFacade> queue = new ArrayList<>();
		for (final File file : tempFiles) {
			queue.add(new SortedTripleFileFacade(file));
		}
		return queue;
	}

	/**
	 * @param aTriple any triple
	 */
	protected abstract void rematchedTriple(Triple aTriple);
}
