package com.st.camera;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class PictureInfoDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_picture_info, container, false);

        TextView infoTextView = view.findViewById(R.id.info_text_view);
        Button closeButton = view.findViewById(R.id.button_close);

        // Retrieve the arguments
        Bundle args = getArguments();
        if (args != null) {
            String path = args.getString("path");
            String name = args.getString("name");
            String size = args.getString("size");
            String date = args.getString("date");
            int width = args.getInt("width");
            int height = args.getInt("height");

            // Display the information
            String info = "Path: " + path + "\n" +
                    "Name: " + name + "\n" +
                    "Size: " + size + "\n" +
                    "Date: " + date + "\n" +
                    "Resolution: " + width + " x " + height;
            infoTextView.setText(info);
        }

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
