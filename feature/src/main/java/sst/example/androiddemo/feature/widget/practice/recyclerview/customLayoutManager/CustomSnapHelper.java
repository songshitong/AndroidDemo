/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific languag`e governing permissions and
 * limitations under the License.
 */

package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;


public class CustomSnapHelper extends SnapHelper {

    @Override
    public int[] calculateDistanceToFinalSnap(
            @NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {

        if (layoutManager instanceof CustomLayoutManger) {
            int[] out = new int[2];
            if (layoutManager.canScrollHorizontally()) {
                out[0] = ((CustomLayoutManger) layoutManager).calculateDistanceToPosition(
                        layoutManager.getPosition(targetView));
                out[1] = 0;
            } else {
                out[0] = 0;
                out[1] = ((CustomLayoutManger) layoutManager).calculateDistanceToPosition(
                        layoutManager.getPosition(targetView));
            }
            return out;
        }
        return null;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX,
                                      int velocityY) {
        return RecyclerView.NO_POSITION;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof CustomLayoutManger) {
            int pos = ((CustomLayoutManger) layoutManager).getFixedScrollPosition();
            if (pos != RecyclerView.NO_POSITION) {
                return layoutManager.findViewByPosition(pos);
            }
        }
        return null;
    }
}
