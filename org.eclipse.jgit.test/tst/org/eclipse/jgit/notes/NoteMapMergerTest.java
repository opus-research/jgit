package org.eclipse.jgit.notes;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.TestRepository.CommitBuilder;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;

public class NoteMapMergerTest extends RepositoryTestCase {
	private TestRepository<Repository> tr;

	private ObjectReader reader;

	private ObjectInserter inserter;

	private NoteMap noRoot;

	private NoteMap empty;

	private NoteMap map_a;

	private NoteMap map_a_b;

	private RevBlob noteAId;

	private String noteAContent;

	private RevBlob noteABlob;

	private RevBlob noteBId;

	private String noteBContent;

	private RevBlob noteBBlob;

	private RevCommit sampleTree_a;

	private RevCommit sampleTree_a_b;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tr = new TestRepository<Repository>(db);
		reader = db.newObjectReader();
		inserter = db.newObjectInserter();

		noRoot = NoteMap.newMap(null, reader);
		empty = NoteMap.newEmptyMap();

		noteAId = tr.blob("a");
		noteAContent = "noteAContent";
		noteABlob = tr.blob(noteAContent);
		sampleTree_a = tr.commit()
				.add(noteAId.name(), noteABlob)
				.create();
		tr.parseBody(sampleTree_a);
		map_a = NoteMap.read(reader, sampleTree_a);

		noteBId = tr.blob("b");
		noteBContent = "noteBContent";
		noteBBlob = tr.blob(noteBContent);
		sampleTree_a_b = tr.commit()
				.add(noteAId.name(), noteABlob)
				.add(noteBId.name(), noteBBlob)
				.create();
		tr.parseBody(sampleTree_a_b);
		map_a_b = NoteMap.read(reader, sampleTree_a_b);
	}

	@Override
	protected void tearDown() throws Exception {
		reader.release();
		inserter.release();
		super.tearDown();
	}

	public void testNoChange() throws IOException {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap result;

		assertEquals(0, countNotes(merger.merge(noRoot, noRoot, noRoot)));
		assertEquals(0, countNotes(merger.merge(empty, empty, empty)));

		result = merger.merge(map_a, map_a, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
	}

	public void testOursEqualsTheirs() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap result;

		assertEquals(0, countNotes(merger.merge(empty, noRoot, noRoot)));
		assertEquals(0, countNotes(merger.merge(map_a, noRoot, noRoot)));

		assertEquals(0, countNotes(merger.merge(noRoot, empty, empty)));
		assertEquals(0, countNotes(merger.merge(map_a, empty, empty)));

		result = merger.merge(noRoot, map_a, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		result = merger.merge(empty, map_a, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		result = merger.merge(map_a_b, map_a, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		result = merger.merge(map_a, map_a_b, map_a_b);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(noteBBlob, result.get(noteBId));
	}

	public void testBaseEqualsOurs() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap result;

		assertEquals(0, countNotes(merger.merge(noRoot, noRoot, empty)));
		result = merger.merge(noRoot, noRoot, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		assertEquals(0, countNotes(merger.merge(empty, empty, noRoot)));
		result = merger.merge(empty, empty, map_a);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		assertEquals(0, countNotes(merger.merge(map_a, map_a, noRoot)));
		assertEquals(0, countNotes(merger.merge(map_a, map_a, empty)));
		result = merger.merge(map_a, map_a, map_a_b);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(noteBBlob, result.get(noteBId));
	}

	public void testBaseEqualsTheirs() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap result;

		assertEquals(0, countNotes(merger.merge(noRoot, empty, noRoot)));
		result = merger.merge(noRoot, map_a, noRoot);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		assertEquals(0, countNotes(merger.merge(empty, noRoot, empty)));
		result = merger.merge(empty, map_a, empty);
		assertEquals(1, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));

		assertEquals(0, countNotes(merger.merge(map_a, noRoot, map_a)));
		assertEquals(0, countNotes(merger.merge(map_a, empty, map_a)));
		result = merger.merge(map_a, map_a_b, map_a);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(noteBBlob, result.get(noteBId));
	}

	public void testAddDifferentNotes() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);
		NoteMap result;

		NoteMap map_a_c = NoteMap.read(reader, sampleTree_a);
		RevBlob noteCId = tr.blob("c");
		RevBlob noteCBlob = tr.blob("noteCContent");
		map_a_c.set(noteCId, noteCBlob);
		map_a_c.writeTree(inserter);

		result = merger.merge(map_a, map_a_b, map_a_c);
		assertEquals(3, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(noteBBlob, result.get(noteBId));
		assertEquals(noteCBlob, result.get(noteCId));
	}

	public void testAddSameNoteDifferentContent() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);
		NoteMap result;

		NoteMap map_a_b1 = NoteMap.read(reader, sampleTree_a);
		String noteBContent1 = noteBContent + "change";
		RevBlob noteBBlob1 = tr.blob(noteBContent1);
		map_a_b1.set(noteBId, noteBBlob1);
		map_a_b1.writeTree(inserter);

		result = merger.merge(map_a, map_a_b, map_a_b1);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(tr.blob(noteBContent + noteBContent1), result.get(noteBId));
	}

	public void testEditSameNoteDifferentContent() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);
		NoteMap result;

		NoteMap map_a1 = NoteMap.read(reader, sampleTree_a);
		String noteAContent1 = noteAContent + "change1";
		RevBlob noteABlob1 = tr.blob(noteAContent1);
		map_a1.set(noteAId, noteABlob1);
		map_a1.writeTree(inserter);

		NoteMap map_a2 = NoteMap.read(reader, sampleTree_a);
		String noteAContent2 = noteAContent + "change2";
		RevBlob noteABlob2 = tr.blob(noteAContent2);
		map_a2.set(noteAId, noteABlob2);
		map_a2.writeTree(inserter);

		result = merger.merge(map_a, map_a1, map_a2);
		assertEquals(1, countNotes(result));
		assertEquals(tr.blob(noteAContent1 + noteAContent2),
				result.get(noteAId));
	}

	public void testEditDifferentNotes() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap result;

		NoteMap map_a1_b = NoteMap.read(reader, sampleTree_a_b);
		String noteAContent1 = noteAContent + "change";
		RevBlob noteABlob1 = tr.blob(noteAContent1);
		map_a1_b.set(noteAId, noteABlob1);
		map_a1_b.writeTree(inserter);

		NoteMap map_a_b1 = NoteMap.read(reader, sampleTree_a_b);
		String noteBContent1 = noteBContent + "change";
		RevBlob noteBBlob1 = tr.blob(noteBContent1);
		map_a_b1.set(noteBId, noteBBlob1);
		map_a_b1.writeTree(inserter);

		result = merger.merge(map_a_b, map_a1_b, map_a_b1);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob1, result.get(noteAId));
		assertEquals(noteBBlob1, result.get(noteBId));
	}

	public void testDeleteDifferentNotes() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);

		NoteMap map_b = NoteMap.read(reader, sampleTree_a_b);
		map_b.set(noteAId, null); // delete note a
		map_b.writeTree(inserter);

		assertEquals(0, countNotes(merger.merge(map_a_b, map_a, map_b)));
	}

	public void testEditDeleteConflict() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);
		NoteMap result;

		NoteMap map_a_b1 = NoteMap.read(reader, sampleTree_a_b);
		String noteBContent1 = noteBContent + "change";
		RevBlob noteBBlob1 = tr.blob(noteBContent1);
		map_a_b1.set(noteBId, noteBBlob1);
		map_a_b1.writeTree(inserter);

		result = merger.merge(map_a_b, map_a_b1, map_a);
		assertEquals(2, countNotes(result));
		assertEquals(noteABlob, result.get(noteAId));
		assertEquals(noteBBlob1, result.get(noteBId));
	}

	public void testMergeLargeTreesWithouthConflict() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);
		NoteMap map1 = createLargeNoteMap("note_1_", "content_1_", 300);
		NoteMap map2 = createLargeNoteMap("note_2_", "content_2_", 300);

		NoteMap result = merger.merge(empty, map1, map2);
		assertEquals(600, countNotes(result));
		// check a few random notes
		assertEquals(tr.blob("content_1_59"), result.get(tr.blob("note_1_59")));
		assertEquals(tr.blob("content_2_10"), result.get(tr.blob("note_2_10")));
		assertEquals(tr.blob("content_2_99"), result.get(tr.blob("note_2_99")));
	}

	private NoteMap createLargeNoteMap(String noteNamePrefix,
			String noteContentPrefix, int notesCount) throws Exception {
		CommitBuilder b = tr.commit();
		for (int i = 0; i < 300; i++) {
			b.add(tr.blob(noteNamePrefix + i).name(),
					tr.blob(noteContentPrefix + i));
		}
		RevCommit tree1 = b.create();
		tr.parseBody(tree1);
		return NoteMap.read(reader, tree1);
	}

	public void testMergeLargeTreesWithConflict() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);
		NoteMap largeTree1 = createLargeNoteMap("note_1_", "content_1_", 300);
		NoteMap largeTree2 = createLargeNoteMap("note_1_", "content_2_", 300);

		NoteMap result = merger.merge(empty, largeTree1, largeTree2);
		assertEquals(300, countNotes(result));
		// check a few random notes
		assertEquals(tr.blob("content_1_59content_2_59"),
				result.get(tr.blob("note_1_59")));
		assertEquals(tr.blob("content_1_10content_2_10"),
				result.get(tr.blob("note_1_10")));
		assertEquals(tr.blob("content_1_99content_2_99"),
				result.get(tr.blob("note_1_99")));
	}

	public void testMergeFanoutAndLeafWithoutConflict() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db, null);

		NoteMap largeTree = createLargeNoteMap("note_1_", "content_1_", 300);
		NoteMap result = merger.merge(map_a, map_a_b, largeTree);
		assertEquals(301, countNotes(result));
	}

	public void testMergeFanoutAndLeafWitConflict() throws Exception {
		NoteMapMerger merger = new NoteMapMerger(db);

		NoteMap largeTree_b1 = createLargeNoteMap("note_1_", "content_1_", 300);
		String noteBContent1 = noteBContent + "change";
		largeTree_b1.set(noteBId, tr.blob(noteBContent1));
		largeTree_b1.writeTree(inserter);

		NoteMap result = merger.merge(map_a, map_a_b, largeTree_b1);
		assertEquals(301, countNotes(result));
		assertEquals(tr.blob(noteBContent + noteBContent1), result.get(noteBId));
	}

	public void testMergeNonNotesWithoutConflict() {
		// TODO
	}

	public void testMergeNonNotesWithConflict() {
		// TODO
	}

	private static int countNotes(NoteMap result) {
		int c = 0;
		Iterator<Note> it = result.iterator();
		while (it.hasNext()) {
			it.next();
			c++;
		}
		return c;
	}
}
