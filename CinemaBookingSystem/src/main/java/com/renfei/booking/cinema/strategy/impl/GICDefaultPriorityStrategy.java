package com.renfei.booking.cinema.strategy.impl;

import com.renfei.booking.cinema.strategy.DefaultPriorityStrategy;
import java.util.ArrayList;
import java.util.List;

public class GICDefaultPriorityStrategy implements DefaultPriorityStrategy {
  @Override
  public int[] calculateDefaultColPriority(int count) {
    int centerLeft = (count - 1) / 2;
    int centerRight = count / 2;
    List<Integer> priority = new ArrayList<>();
    if (count % 2 != 0) {
      priority.add(centerLeft);
      for (int i = 1; i <= centerLeft; i++) {
        priority.add(centerLeft - i);
        priority.add(centerRight + i);
      }
    } else {
      for (int i = 0; i < count / 2; i++) {
        priority.add(centerLeft - i);
        priority.add(centerRight + i);
      }
    }
    return priority.stream().mapToInt(i -> i).toArray();
  }
}
