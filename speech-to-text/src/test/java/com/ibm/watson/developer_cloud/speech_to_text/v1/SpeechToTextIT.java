/*
 * Copyright 2015 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.developer_cloud.speech_to_text.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Assume;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ibm.watson.developer_cloud.WatsonServiceTest;
import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.service.exception.NotFoundException;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Corpus;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Corpus.Status;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Customization;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.KeywordsResult;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognitionJob;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechModel;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechSession;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechSessionStatus;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechWordAlternatives;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Word;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Word.Type;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.WordData;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;

/**
 * Speech to text Integration tests.
 */
public class SpeechToTextIT extends WatsonServiceTest {

  private static final String EN_BROADBAND16K = "en-US_BroadbandModel";
  private static final String SPEECH_RESOURCE = "src/test/resources/speech_to_text/%s";
  private static final String SAMPLE_WAV = String.format(SPEECH_RESOURCE, "sample1.wav");
  private static final String TWO_SPEAKERS_WAV = String.format(SPEECH_RESOURCE, "twospeakers.wav");

  private CountDownLatch lock = new CountDownLatch(1);
  private SpeechToText service;
  private SpeechResults asyncResults;
  private String customizationId;

  /** The expected exception. */
  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.watson.developer_cloud.WatsonServiceTest#setUp()
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    this.customizationId = getProperty("speech_to_text.customization_id");

    String username = getProperty("speech_to_text.username");
    String password = getProperty("speech_to_text.password");

    Assume.assumeFalse("config.properties doesn't have valid credentials.",
        username == null || username.equals(PLACEHOLDER));

    service = new SpeechToText();
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getProperty("speech_to_text.url"));
    service.setDefaultHeaders(getDefaultHeaders());
  }

  /**
   * Test create session.
   */
  @Test
  public void testCreateSession() {
    SpeechSession session = service.createSession().execute();
    try {
      assertNotNull(session);
      assertNotNull(session.getSessionId());
    } finally {
      service.deleteSession(session).execute();
    }
  }

  /**
   * Test create session speech model.
   */
  @Test
  public void testCreateSessionSpeechModel() {
    SpeechSession session = service.createSession(SpeechModel.EN_US_BROADBANDMODEL).execute();
    try {
      assertNotNull(session);
      assertNotNull(session.getSessionId());
    } finally {
      service.deleteSession(session).execute();
    }
  }

  /**
   * Test create session string.
   */
  @Test
  public void testCreateSessionString() {
    SpeechSession session = service.createSession(EN_BROADBAND16K).execute();
    try {
      assertNotNull(session);
      assertNotNull(session.getSessionId());
    } finally {
      service.deleteSession(session).execute();
    }
  }

  /**
   * Test get model.
   */
  @Test
  public void testGetModel() {
    SpeechModel model = service.getModel(EN_BROADBAND16K).execute();
    assertNotNull(model);
    assertNotNull(model.getName());
    assertNotNull(model.getRate());
    assertNotNull(model.getDescription());
  }

  /**
   * Test get models.
   */
  @Test
  public void testGetModels() {
    List<SpeechModel> models = service.getModels().execute();
    assertNotNull(models);
    assertTrue(!models.isEmpty());
  }

  /**
   * Test get recognize status.
   */
  @Test
  public void testGetRecognizeStatus() {
    SpeechSession session = service.createSession(SpeechModel.EN_US_BROADBANDMODEL).execute();
    SpeechSessionStatus status = service.getRecognizeStatus(session).execute();
    try {
      assertNotNull(status);
      assertNotNull(status.getModel());
      assertNotNull(status.getState());
    } finally {
      service.deleteSession(session).execute();
    }
  }

  /**
   * Test recognize audio file.
   */
  @Test
  public void testRecognizeFileString() {
    File audio = new File(SAMPLE_WAV);
    SpeechResults results = service.recognize(audio).execute();
    assertNotNull(results.getResults().get(0).getAlternatives().get(0).getTranscript());
  }

  /**
   * Test recognize multiple speakers.
   */
  @Test
  public void testRecognizeMultipleSpeakers() {
    File audio = new File(TWO_SPEAKERS_WAV);
    RecognizeOptions options = new RecognizeOptions.Builder()
      .continuous(true)
      .interimResults(true)
      .speakerLabels(true)
      .model(SpeechModel.EN_US_NARROWBANDMODEL.getName())
      .contentType(HttpMediaType.AUDIO_WAV)
      .build();

    SpeechResults results = service.recognize(audio, options).execute();
    assertNotNull(results.getSpeakerLabels());
    assertTrue(results.getSpeakerLabels().size() > 0);
  }

  /**
   * Test recognize file string recognize options.
   */
  @Test
  public void testRecognizeFileStringRecognizeOptions() {
    File audio = new File(SAMPLE_WAV);
    String contentType = HttpMediaType.AUDIO_WAV;
    RecognizeOptions options = new RecognizeOptions.Builder().continuous(true).timestamps(true).wordConfidence(true)
        .model(EN_BROADBAND16K).contentType(contentType).profanityFilter(false).build();
    SpeechResults results = service.recognize(audio, options).execute();
    assertNotNull(results.getResults().get(0).getAlternatives().get(0).getTranscript());
    assertNotNull(results.getResults().get(0).getAlternatives().get(0).getTimestamps());
    assertNotNull(results.getResults().get(0).getAlternatives().get(0).getWordConfidences());
  }

  /**
   * Test keyword recognition.
   */
  @Test
  public void testRecognizeKeywords() {
    final String keyword1 = "rain";
    final String keyword2 = "tornadoes";

    final RecognizeOptions options =
        new RecognizeOptions.Builder().contentType("audio/wav").model(SpeechModel.EN_US_BROADBANDMODEL.getName())
            .continuous(true).inactivityTimeout(500).keywords(keyword1, keyword2).keywordsThreshold(0.7).build();

    final File audio = new File(SAMPLE_WAV);
    final SpeechResults results = service.recognize(audio, options).execute();
    final Transcript transcript = results.getResults().get(0);

    assertEquals(2, transcript.getKeywordsResult().size());
    assertTrue(transcript.getKeywordsResult().containsKey(keyword1));
    assertTrue(transcript.getKeywordsResult().containsKey(keyword2));

    assertEquals(1, transcript.getKeywordsResult().get(keyword1).size());
    assertEquals(1, transcript.getKeywordsResult().get(keyword2).size());

    final KeywordsResult result1 = transcript.getKeywordsResult().get(keyword1).get(0);
    assertEquals(keyword1, result1.getNormalizedText());
    assertEquals(0.9, result1.getConfidence(), 0.1);
    assertEquals(5.58, result1.getStartTime(), 1.0);
    assertEquals(6.14, result1.getEndTime(), 1.0);

    final KeywordsResult result2 = transcript.getKeywordsResult().get(keyword2).get(0);
    assertEquals(keyword2, result2.getNormalizedText());
    assertEquals(0.9, result2.getConfidence(), 0.1);
    assertEquals(4.42, result2.getStartTime(), 1.0);
    assertEquals(5.04, result2.getEndTime(), 1.0);
  }

  /**
   * Test recognize webSocket.
   *
   * @throws FileNotFoundException the file not found exception
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testRecognizeWebSocket() throws FileNotFoundException, InterruptedException {
    RecognizeOptions options = new RecognizeOptions.Builder().continuous(true).interimResults(true)
        .inactivityTimeout(40).timestamps(true).maxAlternatives(2).wordAlternativesThreshold(0.5).model(EN_BROADBAND16K)
        .contentType(HttpMediaType.AUDIO_WAV).build();
    FileInputStream audio = new FileInputStream(SAMPLE_WAV);

    service.recognizeUsingWebSocket(audio, options, new BaseRecognizeCallback() {

      @Override
      public void onConnected() {
        System.out.println("onConnected()");
      }

      @Override
      public void onDisconnected() {
        System.out.println("onDisconnected()");
        lock.countDown();
      }

      @Override
      public void onError(Exception e) {
        e.printStackTrace();
        lock.countDown();
      }

      @Override
      public void onTranscription(SpeechResults speechResults) {
        if (speechResults != null && speechResults.isFinal()) {
          asyncResults = speechResults;
        }
      }

    });

    lock.await(2, TimeUnit.MINUTES);
    assertNotNull(asyncResults);

    List<SpeechWordAlternatives> wordAlternatives =
        asyncResults.getResults().get(asyncResults.getResultIndex()).getWordAlternatives();
    assertTrue(wordAlternatives != null && !wordAlternatives.isEmpty());
    assertNotNull(wordAlternatives.get(0).getAlternatives());
  }

  /**
   * Test create recognition job.
   *
   * @throws InterruptedException the interrupted exception
   * @throws FileNotFoundException the file not found exception
   */
  @Test
  public void testCreateRecognitionJob() throws InterruptedException, FileNotFoundException {
    File audio = new File(SAMPLE_WAV);
    RecognitionJob job = service.createRecognitionJob(audio, null, null).execute();
    try {
      assertNotNull(job.getId());
      for (int x = 0; x < 30 && job.getStatus() != RecognitionJob.Status.COMPLETED; x++) {
        Thread.sleep(3000);
        job = service.getRecognitionJob(job.getId()).execute();
      }
      job = service.getRecognitionJob(job.getId()).execute();
      assertEquals(RecognitionJob.Status.COMPLETED, job.getStatus());

      assertNotNull(job.getResults());

    } finally {
      service.deleteRecognitionJob(job.getId());
    }
  }

  /**
   * Test get recognition job with wrong id.
   *
   */
  @Test
  public void testGetRecognitionJobWithWrongId() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("job not found");
    service.getRecognitionJob("foo").execute();
  }

  /**
   * Test get recognition jobs.
   *
   */
  @Test
  public void testGetRecognitionJobs() {
    List<RecognitionJob> jobs = service.getRecognitionJobs().execute();
    assertNotNull(jobs);
  }

  /**
   * Test get customizations.
   */
  @Test
  public void testGetCustomizations() {
    List<Customization> customizations = service.getCustomizations(null).execute();
    assertNotNull(customizations);
    assertTrue(!customizations.isEmpty());
  }

  /**
   * Test get corpora.
   *
   */
  @Test
  @Ignore
  public void testGetCorpora() {
    List<Corpus> result = service.getCorpora(customizationId).execute();
    assertNotNull(result);
  }


  /**
   * Test add text to corpus.
   *
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAddTextToCorpus() {
    service.addTextToCustomizationCorpus(customizationId, "foo3", null, null).execute();
  }

  /**
   * Test get words.
   */
  @Test
  @Ignore
  public void testGetWords() {
    List<WordData> result = service.getWords(customizationId, Type.ALL).execute();
    assertNotNull(result);
    assertTrue(!result.isEmpty());
  }

  /**
   * Test get word.
   */
  public void testGetWord() {
    Word result = service.getWord(customizationId, "string").execute();
    assertNotNull(result);
  }

  /**
   * Test customization.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testCustomization() throws InterruptedException {
    // create customization
    Customization myCustomization =
        service.createCustomization("IEEE-java-sdk-permanent", SpeechModel.EN_US_BROADBANDMODEL, null).execute();
    String id = myCustomization.getId();

    try {
      // Add a corpus file to the model:
      service
          .addTextToCustomizationCorpus(id, "corpus-1", false, new File(String.format(SPEECH_RESOURCE, "corpus1.txt")))
          .execute();

      // Get corpora
      List<Corpus> corpora = service.getCorpora(id).execute();

      assertNotNull(corpora);
      assertTrue(corpora.size() == 1);

      // There is only one corpus so far so choose it
      Corpus corpus = corpora.get(0);

      for (int x = 0; x < 30 && corpus.getStatus() != Status.ANALYZED; x++) {
        corpus = service.getCorpora(id).execute().get(0);
        Thread.sleep(5000);
      }

      assertTrue(corpus.getStatus() == Status.ANALYZED);

      // Now add some user words to the custom model
      service.addWord(id, new Word("IEEE", "IEEE", "I. triple E.")).execute();
      service.addWord(id, new Word("hhonors", "IEEE", "H. honors", "Hilton honors")).execute();

      // Display all words in the words resource (coming from OOVs from the corpus add and the new words just added)
      List<WordData> words = service.getWords(id, Word.Type.ALL).execute();
      assertNotNull(words);

    } finally {
      service.deleteCustomization(id);
    }
  }
}
