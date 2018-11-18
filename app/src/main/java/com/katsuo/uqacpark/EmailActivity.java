package com.katsuo.uqacpark;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.katsuo.uqacpark.base.BaseActivity;

import butterknife.BindView;
import de.cketti.mailto.EmailIntentBuilder;

public class EmailActivity extends BaseActivity {
    private static final String TAG = EmailActivity.class.getSimpleName();

    @BindView(R.id.edit_text_email_subject)
    EditText editTextSubject;

    @BindView(R.id.edit_text_email_message)
    EditText editTextMessage;

    @BindView(R.id.button_send_email)
    Button buttonSendEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_email;
    }

    private void sendEmail() {
        String message = editTextMessage.getText().toString();
        String subject = editTextSubject.getText().toString();

        EmailIntentBuilder.from(this)
                .to("joseph.lurel@gmail.com")
                .subject(subject)
                .body(message)
                .start();
    }
}
