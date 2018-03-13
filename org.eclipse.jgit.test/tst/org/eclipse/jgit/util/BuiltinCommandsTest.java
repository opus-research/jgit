package org.eclipse.jgit.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

public class BuiltinCommandsTest extends RepositoryTestCase {
	private Git git;

	RevCommit initialCommit;

	RevCommit secondCommit;

	class TestCommandFactory implements BuiltinCommandFactory {
		private int prefix;

		public TestCommandFactory(int prefix) {
			this.prefix = prefix;
		}

		@Override
		public BuiltinCommand create(Repository db, InputStream in,
				final OutputStream out) {
			BuiltinCommand cmd = new BuiltinCommand(in, out) {
				@Override
				public int run() throws IOException {
					int b = in.read();
					if (b == -1)
						return b;
					out.write(prefix);
					out.write(b);
					return 1;
				}
			};
			return cmd;
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		git = new Git(db);
		// commit something
		writeTrashFile("Test.txt", "Hello world");
		git.add().addFilepattern("Test.txt").call();
		initialCommit = git.commit().setMessage("Initial commit").call();

		// create a master branch and switch to it
		git.branchCreate().setName("test").call();
		RefUpdate rup = db.updateRef(Constants.HEAD);
		rup.link("refs/heads/test");

		// commit something on the test branch
		writeTrashFile("Test.txt", "Some change");
		git.add().addFilepattern("Test.txt").call();
		secondCommit = git.commit().setMessage("Second commit").call();
	}

	@Test
	public void testBuiltinCleanFilter()
			throws IOException, GitAPIException {
		String builtinCommandName = "jgit://builtin/test/clean";
		Repository.registerCommand(builtinCommandName,
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "clean", builtinCommandName);
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

	@Test
	public void testBuiltinSmudgeFilter() throws IOException, GitAPIException {
		String builtinCommandName = "jgit://builtin/test/smudge";
		Repository.registerCommand(builtinCommandName,
				new TestCommandFactory('s'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge", builtinCommandName);
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));
		assertEquals("Hello again", read("Test.txt"));
		deleteTrashFile("Test.txt");
		git.checkout().addPath("Test.txt").call();
		assertEquals("sHseslslsos sasgsasisn", read("Test.txt"));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));
		deleteTrashFile("Test.bin");
		git.checkout().addPath("Test.bin").call();
		assertEquals("Hello again", read("Test.bin"));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:sHseslslsos sasgsasisn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

	@Test
	public void testBuiltinCleanAndSmudgeFilter() throws IOException, GitAPIException {
		String builtinCommandPrefix = "jgit://builtin/test/";
		Repository.registerCommand(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		Repository.registerCommand(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge", builtinCommandPrefix+"smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));
		assertEquals("Hello again", read("Test.txt"));
		deleteTrashFile("Test.txt");
		git.checkout().addPath("Test.txt").call();
		assertEquals("scsHscsescslscslscsoscs scsascsgscsascsiscsn",
				read("Test.txt"));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));
		deleteTrashFile("Test.bin");
		git.checkout().addPath("Test.bin").call();
		assertEquals("Hello again", read("Test.bin"));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:scsHscsescslscslscsoscs scsascsgscsascsiscsn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

}
