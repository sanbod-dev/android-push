package com.sanbod.push;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

public class PermissionPopupHelper {

    /**
     * Shows a custom popup dialog explaining why the permission is needed.
     *
     * @param activity          The Activity context.
     * @param layoutResId       The layout resource ID provided by the host app.
     * @param positiveButtonRes The resource ID for the positive button text.
     * @param negativeButtonRes The resource ID for the negative button text (can be 0 if not needed).
     * @param onUserResponse    Callback invoked when the user makes a decision.
     */
    public static void showPermissionExplanationDialog(Activity activity,
                                                       @LayoutRes int layoutResId,
                                                       @StringRes int positiveButtonRes,
                                                       @StringRes int negativeButtonRes,
                                                       PermissionDialogCallback onUserResponse) {
        // Inflate the custom view provided by the host app
        View customView = LayoutInflater.from(activity).inflate(layoutResId, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(customView)
                .setCancelable(false)
                .setPositiveButton(positiveButtonRes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onUserResponse != null) {
                            onUserResponse.onPositive();
                        }
                    }
                });

        // Optionally add a negative button if resource id is provided
        if (negativeButtonRes != 0) {
            builder.setNegativeButton(negativeButtonRes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (onUserResponse != null) {
                        onUserResponse.onNegative();
                    }
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public interface PermissionDialogCallback {
        void onPositive();
        void onNegative();
    }
}

