/*
 * Copyright 2014 Uwe Trottmann
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.items.SearchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by florianmski's traktoid TraktManager. This class is used to hold running tasks, so it
 * can execute independently from a running activity (so the application can still be used while
 * the
 * update continues). A plain AsyncTask could do this, too, but here we can also restrict it to one
 * task running at a time.
 */
public class TaskManager {

    private static TaskManager _instance;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private AddShowTask mAddTask;

    private JsonExportTask mBackupTask;

    private Context mContext;

    private TaskManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized TaskManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new TaskManager(context);
        }
        return _instance;
    }

    public synchronized void performAddTask(SearchResult show) {
        List<SearchResult> wrapper = new ArrayList<>();
        wrapper.add(show);
        performAddTask(wrapper, false);
    }

    public synchronized void performAddTask(final List<SearchResult> shows,
            final boolean isSilent) {
        if (!isSilent) {
            // notify user here already
            if (shows.size() == 1) {
                // say title of show
                SearchResult show = shows.get(0);
                Toast.makeText(mContext,
                        mContext.getString(R.string.add_started, show.title),
                        Toast.LENGTH_SHORT).show();
            } else {
                // generic adding multiple message
                Toast.makeText(
                        mContext,
                        R.string.add_multiple,
                        Toast.LENGTH_SHORT).show();
            }
        }

        // add the show(s) to a running add task or create a new one
        if (!isAddTaskRunning() || !mAddTask.addShows(shows)) {
            // ensure this is called on our main thread (AsyncTask needs access to it)
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAddTask = (AddShowTask) Utils.executeInOrder(
                            new AddShowTask(mContext, shows, isSilent));
                }
            });
        }
    }

    public boolean isAddTaskRunning() {
        return !(mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED);
    }

    /**
     * If no {@link AddShowTask} or {@link JsonExportTask} created by this {@link
     * com.battlelancer.seriesguide.util.TaskManager} is running a
     * {@link JsonExportTask} is scheduled in silent mode.
     */
    public void tryBackupTask() {
        if (!isAddTaskRunning()
                && (mBackupTask == null || mBackupTask.getStatus() == AsyncTask.Status.FINISHED)) {
            mBackupTask = new JsonExportTask(mContext, null, null, false, true);
            Utils.executeInOrder(mBackupTask);
        }
    }
}
