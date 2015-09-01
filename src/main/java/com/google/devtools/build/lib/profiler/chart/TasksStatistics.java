// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.profiler.chart;

import com.google.devtools.build.lib.profiler.ProfileInfo.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data container to aggregate execution time statistics of multiple tasks grouped by some name.
 */
class TasksStatistics {
  public final String name;
  public final int count;
  public final long minNanos;
  public final long maxNanos;
  public final double meanNanos;
  public final double medianNanos;
  /** Standard deviation of the execution time in milliseconds since computation in nanoseconds can
   * overflow.
   */
  public final double standardDeviationMillis;

  public final long totalNanos;

  public TasksStatistics(
      String name,
      int count,
      long min,
      long max,
      double mean,
      double median,
      double standardDeviation,
      long total) {
    this.name = name;
    this.count = count;
    this.minNanos = min;
    this.maxNanos = max;
    this.meanNanos = mean;
    this.medianNanos = median;
    this.standardDeviationMillis = standardDeviation;
    this.totalNanos = total;
  }

  public double minimumMillis() {
    return toMilliSeconds(minNanos);
  }

  public double maximumMillis() {
    return toMilliSeconds(maxNanos);
  }

  public double meanMillis() {
    return toMilliSeconds(meanNanos);
  }

  public double medianMillis() {
    return toMilliSeconds(medianNanos);
  }

  public double totalMillis() {
    return toMilliSeconds(totalNanos);
  }

  /**
   * @param name
   * @param tasks
   * @return The set of statistics grouped in this class, computed from a list of {@link Task}s.
   */
  public static TasksStatistics create(String name, List<Task> tasks) {
    Collections.sort(
        tasks,
        new Comparator<Task>() {
          @Override
          public int compare(Task o1, Task o2) {
            return Long.compare(o1.duration, o2.duration);
          }
        });
    int count = tasks.size();
    long min = tasks.get(0).duration;
    long max = tasks.get(count - 1).duration;

    int midIndex = count / 2;
    double median =
        tasks.size() % 2 == 0
            ? (tasks.get(midIndex).duration + tasks.get(midIndex - 1).duration) / 2.0
            : tasks.get(midIndex).duration;

    // Compute standard deviation with a shift to avoid catastrophic cancellation
    // and also do it in milliseconds, as in nanoseconds it overflows
    long sum = 0L;
    double sumOfSquaredShiftedMillis = 0L;
    final long shift = min;

    for (Task task : tasks) {
      sum += task.duration;
      double taskDurationShiftMillis = toMilliSeconds(task.duration - shift);
      sumOfSquaredShiftedMillis += taskDurationShiftMillis * taskDurationShiftMillis;
    }
    double sumShiftedMillis = toMilliSeconds(sum - count * shift);

    double mean = (double) sum / count;

    double standardDeviation =
        Math.sqrt(
            (sumOfSquaredShiftedMillis - (sumShiftedMillis * sumShiftedMillis) / count) / count);

    return new TasksStatistics(name, count, min, max, mean, median, standardDeviation, sum);
  }

  static double toMilliSeconds(double nanoseconds) {
    return nanoseconds / 1000000.0;
  }
}
