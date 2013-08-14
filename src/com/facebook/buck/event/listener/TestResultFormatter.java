/*
 * Copyright 2013-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.event.listener;

import com.facebook.buck.rules.TestCaseSummary;
import com.facebook.buck.rules.TestResultSummary;
import com.facebook.buck.rules.TestResults;
import com.facebook.buck.util.Ansi;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class TestResultFormatter {

  private final Ansi ansi;

  public TestResultFormatter(Ansi ansi) {
    this.ansi = Preconditions.checkNotNull(ansi);
  }

  public void runStarted(ImmutableList.Builder<String> addTo,
      boolean isRunAllTests,
      ImmutableList<String> targetNames) {

    String targetsBeingTested;
    if (isRunAllTests) {
      targetsBeingTested = "ALL TESTS";
    } else {
      targetsBeingTested = Joiner.on(' ').join(targetNames);
    }
    addTo.add(String.format("TESTING %s", targetsBeingTested));
  }

  /** Writes a detailed summary that ends with a trailing newline. */
  public void reportResult(ImmutableList.Builder<String> addTo, TestResults results) {
    for (TestCaseSummary testCase : results.getTestCases()) {
      addTo.add(testCase.getOneLineSummary(ansi));

      if (testCase.isSuccess()) {
        continue;
      }

      for (TestResultSummary testResult : testCase.getTestResults()) {
        if (!testResult.isSuccess()) {
          addTo.add(String.format("FAILURE %s: %s",
              testResult.getTestName(),
              testResult.getMessage()));
          addTo.add(testResult.getStacktrace());

          if (testResult.getStdOut() != null) {
            addTo.add("====STANDARD OUT====", testResult.getStdOut());
          }

          if (testResult.getStdErr() != null) {
            addTo.add("====STANDARD ERR====", testResult.getStdErr());
          }
        }
      }
    }
  }

  public void runComplete(ImmutableList.Builder<String> addTo, List<TestResults> completedResults) {
    // Print whether each test succeeded or failed.
    boolean isAllTestsPassed = true;
    int numFailures = 0;
    for (TestResults summary : completedResults) {
      if (!summary.isSuccess()) {
        isAllTestsPassed = false;
        numFailures += summary.getFailureCount();
      }
    }

    // Print the summary of the test results.
    if (completedResults.isEmpty()) {
      addTo.add(ansi.asHighlightedFailureText("NO TESTS RAN"));
    } else if (isAllTestsPassed) {
      addTo.add(ansi.asHighlightedSuccessText("TESTS PASSED"));
    } else {
      addTo.add(ansi.asHighlightedFailureText(
          String.format("TESTS FAILED: %d Failures", numFailures)));
    }
  }
}
