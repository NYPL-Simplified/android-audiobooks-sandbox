package com.example.android.uamp.model;

//import com.example.android.uamp.TestSetupHelper;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;

import android.content.Context;
import android.util.Log;

import static org.junit.Assert.*;

/**
 * Created by daryachernikhova on 5/2/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManifestReaderTest {
	private static final String TAG = ManifestReaderTest.class.getSimpleName();

	@Mock
	Context context; // cannot be static, or will return null
	// if want something like context.getFilesDir() instead of "new File", then see http://stackoverflow.com/questions/9898634/how-to-provide-data-files-for-android-unit-tests
	// or try  @Rule
	// public TemporaryFolder tempFolder = new TemporaryFolder();
	// or try  @Mock
	//FileOutputStream fileOutputStream;


	private static final String BASE_PATH = resolveBasePath(); // e.g. "./mymodule/src/test/resources/";


	private static String resolveBasePath() {
		final String path = "./mobile/src/test/assets/";
		if (Arrays.asList(new File("./").list()).contains("mobile")) {
			return path; // version for call unit tests from Android Studio
		}
		return "../" + path; // version for call unit tests from terminal './gradlew test'
	}

	static File testAssetDirectory;


	private static ManifestReader manifestReader = new ManifestReader();  // might someday need to mock it?  Dummy dummy = mock(Dummy.class);

	/*
	public static String readFile(@Nonnull final String path) throws IOException {
		final StringBuilder sb = new StringBuilder();
		String strLine;
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
			while ((strLine = reader.readLine()) != null) {
				sb.append(strLine);
			}
		} catch (final IOException ignore) {
			//ignore
		}
		return sb.toString();
	}
	*/


	@BeforeClass
	public static void setupManifest() throws Exception {
		System.out.println(TAG + "BASE_PATH=" + BASE_PATH);
		testAssetDirectory = new File(BASE_PATH, "21_gun_salute");
		System.out.println(TAG + "file path=" + testAssetDirectory.getAbsolutePath());

		//when(context.openFileOutput(anyString(), anyInt())).thenReturn(fileOutputStream);

		/* doesn't work because I can't call Log without mocking it, but good example on how to use an answer
		for my, not native classes.
		Answer<String> answer = new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				String string_1 = invocation.getArgumentAt(0, String.class);
				String string_2 = invocation.getArgumentAt(1, String.class);

				System.out.println(string_1 + ", " + string_2);
				return string_1 + ", " + string_2;
			}
		};
		when(Log.e(any(String.class), any(String.class))).thenAnswer(answer);
		 */
		manifestReader.LOGGING = false;
	}


	@AfterClass
	public static void teardownManifest() throws Exception {
		// do nothing
	}


	@Test
	public void readManifest() throws Exception {
		String fileContents = manifestReader.readManifest(testAssetDirectory);
		assertTrue(fileContents.contains("metadata\": {"));
	}


	@Test
	public void parseManifest() throws Exception {
		// cheating a bit here, using another method to read the sample manifest from file
		String fileContents = manifestReader.readManifest(testAssetDirectory);
		ManifestModel manifest = manifestReader.parseManifest(fileContents);
		assertEquals(manifest.getMetadata().getTitle(), "21 Gun Salute");
	}

}