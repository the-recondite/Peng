package com.example.peng;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
public class ViewHolder extends RecyclerView.ViewHolder {
    public ImageView photoImageView;
    public TextView messageTextView;
    public TextView nameTextView;

    public ViewHolder(View itemView) {
        super(itemView);
        photoImageView = itemView.findViewById(R.id.photoImageView);
        messageTextView = itemView.findViewById(R.id.messageTextView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
    }

    public void setMessageTextView(TextView messageTextView) {
        this.messageTextView = messageTextView;
    }

    public void setNameTextView(TextView nameTextView) {
        this.nameTextView = nameTextView;
    }

    public void setPhotoImageView(ImageView photoImageView) {
        this.photoImageView = photoImageView;
    }

    public ImageView getPhotoImageView() {
        return photoImageView;
    }

    public TextView getMessageTextView() {
        return messageTextView;
    }

    public TextView getNameTextView() {
        return nameTextView;
    }
}
