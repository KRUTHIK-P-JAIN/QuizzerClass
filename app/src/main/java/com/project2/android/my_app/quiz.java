package com.project2.android.my_app;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

//import androidx.appcompat.R;

public class quiz extends AppCompatActivity {
    Boolean storage_permission = true;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();  // to access firestore

    String phone, name; // to store entered phone number in sign up screen
    Map<String, Object> user; // to store all user data entered and store in firestore

    /* layouts L_sign_up, login to make visible or gone according to use,
    qtn_lyttP, qtn_lyttC, qtn_lyttM is physics ,chemistry, maths layout in which respective questions are displayed*/
    LinearLayout L_sign_up, login, start_quiz, subjects;

    View inner_qtn_lyt; // to refer qtn_lyt.xml which contains card view of question and options
    int i;
    int j;
    int totalQuestions;
    String sem;
    public static int Phy_Questions = 0, Che_Questions = 0, Mat_Questions = 0; // to store how many questions

    // to store reference of each radio button of all questions, to validate which radio button is checked(selected)
    // 1000 questions each has 5 options, 5th is not visible as it contains answer(it is required to validate with selected option)
    RadioButton[][] Opt = new RadioButton[1000][5];

    /* answer: to store correct answer in option 5 of each question during validation
       s_phone: to store phone number entered in sign in screen
       codeByFirebase: to store otp came from firebase, to check with entered otp
       stop: to store stoping time*/
    String answer, s_phone, codeByFirbase, stop;
    String selectedSubject;
    Button reg;
    TextInputEditText l_phone, l_password, Name, Phone, Sem, Password;
    ProgressBar s_progress;
    Boolean start = false; // to know user entered quiz or not
    public static Boolean call = true; // to make false when phone call comes
    Boolean in_signup = false; // to know user in sign up or not
    Boolean back = false;
    Boolean profile = false;
    static int Result; // to store result of subjects
    public static String[] Responses = new String[1000]; // 1000 options(one option from a question)
    private static final long START_TIME_IN_MILLIS = 601000; //3 hr 1 min in milliseconds
    private TextView mTextViewCountDown, phyTxt, cheTxt, matTxt;
    private CountDownTimer mCountDownTimer;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;
    LinearLayout quiz, questionPaper;
    Boolean verified;
    Handler handler;
    Runnable runnable;
    Boolean startQuizPage = false;
    Boolean entered = false;
    boolean testScheduleded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);


        if (ContextCompat.checkSelfPermission(quiz.this, Manifest.permission.READ_PHONE_STATE) +
                ContextCompat.checkSelfPermission(quiz.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //when permission not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(quiz.this, Manifest.permission.READ_PHONE_STATE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(quiz.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(quiz.this, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
            } else {
                ActivityCompat.requestPermissions(quiz.this, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
            }
        }

        // to hide navigation bar during quiz
        View quizPage = findViewById(R.id.quiz);
        quizPage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        mTextViewCountDown = (TextView) findViewById(R.id.timer_txt);

        // login() is in try block because parse function of SimpleDateFormat is used in it
        try {
            login();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        CardView Submit = findViewById(R.id.submit);
        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(quiz.this);
                if (storage_permission) {
                    builder.setMessage("Have you attended ALL THE QUESTIONS, Are you sure you want to SUBMIT?" +
                            "\n\nif YES: your response will be stored in " + selectedSubject + "_Responses.pdf");
                } else {
                    builder.setMessage("Have you attended ALL THE QUESTIONS, Are you sure you want to SUBMIT?");
                }
                builder.setCancelable(false)
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                validate_quiz();
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // to hide navigation bar during quiz
                                        View quizPage = findViewById(R.id.quiz);
                                        quizPage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                    }
                                }, 5000);
                            }
                        });
                final AlertDialog dialog = builder.create();

                //show dialog box
                dialog.show();

            }
        });


    }


    private void validate_quiz() {

        Result = 0;
        for (j = 0; j < totalQuestions; j++)
            Responses[j] = "not answered";

        for (i = 0; i < totalQuestions; i++) {
            for (j = 0; j < 4; j++) {
                //ans = j;
                if (Opt[i][j].isChecked()) {

                    Responses[i] = Opt[i][j].getText().toString();
                    String res = Responses[i];
                    answer = Opt[i][4].getText().toString().substring(6);
                    //Toast.makeText(quiz.this, ""+ answer + "  " + Responses[i] + "  "+ res.equals(answer), Toast.LENGTH_SHORT).show();
                    if (Integer.parseInt(answer) == j + 1)
                        Result += 1;

                    //Toast.makeText(quiz.this, ""+ answer + "  " + Responses[i] +"  "+Result, Toast.LENGTH_SHORT).show();


                }
            }
        }

        result();
    }

    private void login() throws ParseException {

        login = findViewById(R.id.login);
        login.setVisibility(View.VISIBLE);

        final ProgressBar l_progress = findViewById(R.id.l_progress);
        Button go = findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                l_phone = findViewById(R.id.l_phone);
                l_password = findViewById(R.id.l_password);

                int valid = 1;

                s_phone = l_phone.getText().toString().trim();
                final String s_password = l_password.getText().toString().trim();

                if (s_phone.isEmpty()) {
                    valid = 0;
                    l_phone.setError("No empty fields are allowed");
                } else
                    l_phone.setError(null);

                if (s_password.isEmpty()) {
                    valid = 0;
                    l_password.setError("No empty fields are allowed");
                } else
                    l_password.setError(null);


                if (valid == 1) {

                    db.collection("QuizzerClassUsers").document(s_phone).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {

                                    if (document.get("Password").toString().equals(s_password)) {

                                        sem = document.get("Sem").toString();
                                        hideKeyboard();
                                        l_progress.setVisibility(View.VISIBLE);
                                        login.setVisibility(View.GONE);

                                        l_phone.setText("");
                                        l_password.setText("");
                                        l_progress.setVisibility(View.GONE);
                                        Toast.makeText(quiz.this, "Welcome", Toast.LENGTH_SHORT).show();
                                        subjectsListLayout();

                                    } else
                                        Toast.makeText(quiz.this, "Phone number or Password is incorrect", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(quiz.this, "No such user", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(quiz.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            }

        });

        Button b_signup = findViewById(R.id.b_signup);
        b_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                login.setVisibility(View.GONE);
                L_sign_up = findViewById(R.id.L_sign_up);
                L_sign_up.setVisibility(View.VISIBLE);

                Name = findViewById(R.id.name);
                Phone = findViewById(R.id.phone);
                Sem = findViewById(R.id.sem);
                Password = findViewById(R.id.password);

                Name.setEnabled(true);
                Phone.setEnabled(true);
                Sem.setEnabled(true);
                Password.setEnabled(true);

                s_progress = findViewById(R.id.s_progress);
                s_progress.setVisibility(View.GONE);
                in_signup = true;

                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(quiz.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    //return;
                } else {
                    Phone.setText(telephonyManager.getLine1Number().substring(2));
                    Phone.setEnabled(false);
                }

                reg = findViewById(R.id.register);
                reg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //if (reg.getText().toString().equals("VERIFY")) {
                        Name = findViewById(R.id.name);
                        name = Name.getText().toString().trim();
                        Phone = findViewById(R.id.phone);
                        phone = Phone.getText().toString().trim();
                        Sem = findViewById(R.id.sem);
                        String sem = Sem.getText().toString().trim();
                        Password = findViewById(R.id.password);
                        String password = Password.getText().toString().trim();

                        int valid = 1;

                        if (name.isEmpty()) {
                            valid = 0;
                            Name.setError("No empty fields are allowed");
                        } else
                            Name.setError(null);

                        if (phone.isEmpty()) {
                            valid = 0;
                            Phone.setError("No empty fields are allowed");
                        } else if (phone.length() != 10 || !phone.matches("^[6-9]\\d{9}$")) {
                            valid = 0;
                            Phone.setError("Invalid Phone number");
                        } else
                            Phone.setError(null);

                        if (sem.isEmpty()) {
                            valid = 0;
                            Sem.setError("No empty fields are allowed");
                        } else if (sem.length() > 1) {
                            valid = 0;
                            Sem.setError("Invalid Sem");
                        } else
                            Sem.setError(null);

                        if (password.isEmpty()) {
                            valid = 0;
                            Password.setError("No empty fields are allowed");
                        } else if (password.length() < 6) {
                            valid = 0;
                            Password.setError("Minimum password length must be 6 charcaters");
                        } else if (true) {
                            for (i = 0; i < password.length(); i++) {
                                if (password.charAt(i) == ' ') {
                                    valid = 0;
                                    Password.setError("White spaces are not allowed");
                                    break;
                                }
                            }
                        } else
                            Password.setError(null);


                        if (valid == 1) {
                            s_progress.setVisibility(View.VISIBLE);
                            /*db.collection("QuizzerClassUsers").document(phone).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Toast.makeText(quiz.this, "You have already registered  " + documentSnapshot.getString("Sem"), Toast.LENGTH_LONG).show();
                                    s_progress.setVisibility(View.GONE);
                                    L_sign_up.setVisibility(View.GONE);
                                    in_signup = false;
                                    login.setVisibility(View.VISIBLE);

                                    Name.setText("");
                                    Sem.setText("");
                                    Phone.setText("");
                                    Password.setText("");

                                    l_phone = findViewById(R.id.l_phone);
                                    l_password = findViewById(R.id.l_password);
                                    l_phone.setText("");
                                    l_phone.setError(null);
                                    l_password.setText("");
                                    l_password.setError(null);
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {*/

                            user = new HashMap<>();
                            user.put("Name", name);
                            user.put("Phone", phone);
                            user.put("Sem", sem);
                            user.put("Password", password);

                            //sendVerificationCodeToUser(phone);
                            //verified = sendSMS(phone);

                            db.collection("QuizzerClassUsers").document(phone).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot doc = task.getResult();
                                        if (doc.exists()) {
                                            Toast.makeText(quiz.this, "you have already registered", Toast.LENGTH_SHORT).show();
                                            s_progress.setVisibility(View.GONE);
                                            L_sign_up.setVisibility(View.GONE);
                                            in_signup = false;
                                            login.setVisibility(View.VISIBLE);

                                            Name.setText("");
                                            Sem.setText("");
                                            Phone.setText("");
                                            Password.setText("");

                                            l_phone = findViewById(R.id.l_phone);
                                            l_password = findViewById(R.id.l_password);
                                            l_phone.setText("");
                                            l_phone.setError(null);
                                            l_password.setText("");
                                            l_password.setError(null);
                                        } else {
                                            db.collection("QuizzerClassUsers").document(phone).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(quiz.this, "User Created", Toast.LENGTH_SHORT).show();
                                                    /*try {
                                                        Toast.makeText(quiz.this, "" + haash(password), Toast.LENGTH_SHORT).show();
                                                    } catch (NoSuchAlgorithmException e) {
                                                        e.printStackTrace();
                                                    } catch (InvalidKeySpecException e) {
                                                        e.printStackTrace();
                                                    }*/
                                                    s_progress.setVisibility(View.GONE);
                                                    L_sign_up.setVisibility(View.GONE);
                                                    in_signup = false;
                                                    login.setVisibility(View.VISIBLE);

                                                    Name.setText("");
                                                    Sem.setText("");
                                                    Phone.setText("");
                                                    Password.setText("");

                                                    l_phone = findViewById(R.id.l_phone);
                                                    l_password = findViewById(R.id.l_password);
                                                    l_phone.setText("");
                                                    l_phone.setError(null);
                                                    l_password.setText("");
                                                    l_password.setError(null);

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(quiz.this, "User not Created " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    s_progress.setVisibility(View.GONE);
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                            //}
                            //});

                        }
                        /*} else {
                            s_progress.setVisibility(View.VISIBLE);
                            Phone_Otp = findViewById(R.id.phone_otp);
                            String otp = Phone_Otp.getText().toString();
                            if (!otp.isEmpty()) {
                                verifyCode(otp);
                            } else {
                                Phone_Otp.setError("Enter otp");
                                s_progress.setVisibility(View.GONE);
                            }*/

                            /*if (verified) {
                                Phone_Otp = findViewById(R.id.phone_otp);
                                String otp = Phone_Otp.getText().toString().trim();

                                if (!otp.isEmpty() && "123456".equals(otp)) {
                                    //Toast.makeText(quiz.this, "User Created", Toast.LENGTH_SHORT).show();

                                    db.collection("Users").document(phone).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(quiz.this, "User Created", Toast.LENGTH_SHORT).show();
                                            s_progress.setVisibility(View.GONE);
                                            L_sign_up.setVisibility(View.GONE);
                                            in_signup = false;
                                            login.setVisibility(View.VISIBLE);

                                            Name.setText("");
                                            College.setText("");
                                            branch.setText("");
                                            Email.setText("");
                                            Sem.setText("");
                                            Phone_Otp.setText("");
                                            Phone_Otp.setVisibility(View.GONE);
                                            reg.setText("VERIFY");
                                            Phone.setText("");
                                            Password.setText("");
                                            C_Password.setText("");

                                            l_phone = findViewById(R.id.l_phone);
                                            l_password = findViewById(R.id.l_password);
                                            l_phone.setText("");
                                            l_phone.setError(null);
                                            l_password.setText("");
                                            l_password.setError(null);

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(quiz.this, "User not Created " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            s_progress.setVisibility(View.GONE);

                                        }
                                    });
                                } else
                                    Toast.makeText(quiz.this, "Invalid OTP", Toast.LENGTH_SHORT).

                                            show();
                            }*/
                    }
                });

                TextView have_acc = findViewById(R.id.have_acc);
                have_acc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        L_sign_up.setVisibility(View.GONE);
                        in_signup = false;
                        login.setVisibility(View.VISIBLE);

                        l_phone = findViewById(R.id.l_phone);
                        l_password = findViewById(R.id.l_password);

                        Name = findViewById(R.id.name);
                        Phone = findViewById(R.id.phone);
                        Sem = findViewById(R.id.sem);
                        //Phone_Otp = findViewById(R.id.phone_otp);
                        Password = findViewById(R.id.password);

                        Name.setText("");
                        Name.setError(null);
                        Sem.setText("");
                        Sem.setError(null);
                        Phone.setText("");
                        Phone.setError(null);
                        //reg.setText("VERIFY");
                        Password.setText("");
                        Password.setError(null);

                        l_phone.setText("");
                        l_phone.setError(null);
                        l_password.setText("");
                        l_password.setError(null);
                    }
                });

            }
        });
    }

    private String haash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[16];
        Random random = new Random();
        random.nextBytes(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = f.generateSecret(spec).getEncoded();
        Base64.Encoder enc = Base64.getEncoder();
        return "salt: " + enc.encodeToString(salt) + "hash: " + enc.encodeToString(hash);
    }

    private void subjectsListLayout() {

        ImageView profile = findViewById(R.id.profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                profilee();
            }
        });
        List subjects_list = new ArrayList();
        db.collection("QuizzerClass").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    int count = 0;

                    for (QueryDocumentSnapshot querySnapshot : task.getResult()) {
                        count++;
                        if (querySnapshot.getId().split("_")[0].equals(sem)) {
                            //Toast.makeText(quiz.this, "" + querySnapshot.getId(), Toast.LENGTH_SHORT).show();
                            String sub = querySnapshot.getId().split("_")[1];
                            if (!subjects_list.contains(sub))
                                subjects_list.add(sub);
                            ListView sub_list = findViewById(R.id.sub_list);
                            //String txt = tyu.getText().toString();
                            ArrayAdapter adapter = new ArrayAdapter(quiz.this, android.R.layout.simple_list_item_1, subjects_list);
                            sub_list.setAdapter(adapter);
                        }
                    }
                } else
                    Toast.makeText(quiz.this, "" + task.getResult(), Toast.LENGTH_SHORT).show();
            }
        });

        subjects = findViewById(R.id.subjects);
        subjects.setVisibility(View.VISIBLE);
        ListView sub_list = findViewById(R.id.sub_list);
        sub_list.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (subjects_list.size() == 0)
                    sub_list.setVisibility(View.GONE);
            }
        }, 3000);

        sub_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSubject = subjects_list.get(i).toString();
                db.collection("QuizzerClassUsers").document(s_phone).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.get(selectedSubject + "_marks") == null) {
                            db.collection("QuizzerClass").orderBy("Question_no").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        questionPaper = findViewById(R.id.questionPaper);
                                        questionPaper.removeAllViews();
                                        View question;
                                        //Toast.makeText(, "", Toast.LENGTH_SHORT).show();
                                        int i = 0;
                                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                            if (documentSnapshot.getId().split("_")[0].equals(sem)) {
                                                if (documentSnapshot.getId().split("_")[1].equals(selectedSubject)) {

                                                    Toast.makeText(quiz.this, "All questions loaded", Toast.LENGTH_SHORT).show();
                                                    question = getLayoutInflater().inflate(R.layout.qtn_lyt, null, false);
                                                    questionPaper.addView(question);

                                                    TextView qtn_no = question.findViewById(R.id.c_qutnn_no);
                                                    TextView qtn = question.findViewById(R.id.c_qutnn);
                                                    //ZoomInImageView q_img = question.findViewById(R.id.q_img);
                                                    RadioButton r1 = question.findViewById(R.id.radioButton);
                                                    Opt[i][0] = r1;
                                                    RadioButton r2 = question.findViewById(R.id.radioButton1);
                                                    Opt[i][1] = r2;
                                                    RadioButton r3 = question.findViewById(R.id.radioButton2);
                                                    Opt[i][2] = r3;
                                                    RadioButton r4 = question.findViewById(R.id.radioButton3);
                                                    Opt[i][3] = r4;
                                                    RadioButton r5 = question.findViewById(R.id.answer);
                                                    Opt[i][4] = r5;

                                                    qtn_no.setVisibility(View.VISIBLE);

                                                    qtn_no.setText(documentSnapshot.getId().split("_")[2] + ".");
                                                    qtn.setText(documentSnapshot.getData().get("Question").toString());

                                                    r5.setText(documentSnapshot.getData().get("Answer").toString());
                                                    r1.setText(documentSnapshot.getData().get("Option1").toString() + " ");
                                                    r2.setText(documentSnapshot.getData().get("Option2").toString() + " ");
                                                    r3.setText(documentSnapshot.getData().get("Option3").toString() + " ");
                                                    r4.setText(documentSnapshot.getData().get("Option4").toString() + " ");
                                                    i++;

                                                    //Toast.makeText(quiz.this, "asdfgh", Toast.LENGTH_SHORT).show();


                                                }
                                                //Toast.makeText(quiz.this, ""+documentSnapshot.getId().split("_")[1], Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        totalQuestions = i;
                                        //Toast.makeText(quiz.this, ""+totalQuestions, Toast.LENGTH_SHORT).show();
                                    } else
                                        Toast.makeText(quiz.this, "" + task.getResult(), Toast.LENGTH_SHORT).show();
                                }
                            });

                            entered = false;
                        } else entered = true;
                    }
                });

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        subjects.setVisibility(View.GONE);
                        startQuizLayout();
                    }
                }, 1000);
            }
        });
    }

    private void profilee() {

        subjects.setVisibility(View.GONE);
        L_sign_up = findViewById(R.id.L_sign_up);
        TextView logo = findViewById(R.id.logo);
        logo.setText("Update,");
        TextView slogan = findViewById(R.id.slogan);
        slogan.setText("");
        Button update = findViewById(R.id.register);
        update.setText("UPDATE");
        findViewById(R.id.have_acc).setVisibility(View.GONE);
        L_sign_up.setVisibility(View.VISIBLE);
        profile = true;

        Name = findViewById(R.id.name);
        Phone = findViewById(R.id.phone);
        Sem = findViewById(R.id.sem);
        Password = findViewById(R.id.password);

        Name.setEnabled(true);
        Phone.setEnabled(true);
        Sem.setEnabled(true);
        Password.setEnabled(true);

        s_progress = findViewById(R.id.s_progress);
        s_progress.setVisibility(View.GONE);
        //in_signup = true;

        db.collection("QuizzerClassUsers").document(s_phone).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Name.setText(documentSnapshot.getString("Name"));
                Phone.setText(documentSnapshot.getString("Phone"));
                Phone.setEnabled(false);
                Sem.setText(documentSnapshot.getString("Sem"));
                Password.setText(documentSnapshot.getString("Password"));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if (reg.getText().toString().equals("VERIFY")) {
                Name = findViewById(R.id.name);
                name = Name.getText().toString().trim();
                Phone = findViewById(R.id.phone);
                phone = Phone.getText().toString().trim();
                Sem = findViewById(R.id.sem);
                sem = Sem.getText().toString().trim();
                Password = findViewById(R.id.password);
                String password = Password.getText().toString().trim();

                int valid = 1;

                if (name.isEmpty()) {
                    valid = 0;
                    Name.setError("No empty fields are allowed");
                } else
                    Name.setError(null);

                if (phone.isEmpty()) {
                    valid = 0;
                    Phone.setError("No empty fields are allowed");
                } else if (phone.length() != 10 || !phone.matches("^[6-9]\\d{9}$")) {
                    valid = 0;
                    Phone.setError("Invalid Phone number");
                } else
                    Phone.setError(null);

                if (sem.isEmpty()) {
                    valid = 0;
                    Sem.setError("No empty fields are allowed");
                } else if (sem.length() > 1) {
                    valid = 0;
                    Sem.setError("Invalid Sem");
                } else
                    Sem.setError(null);

                if (password.isEmpty()) {
                    valid = 0;
                    Password.setError("No empty fields are allowed");
                } else if (password.length() < 6) {
                    valid = 0;
                    Password.setError("Minimum password length must be 6 charcaters");
                } else if (true) {
                    for (i = 0; i < password.length(); i++) {
                        if (password.charAt(i) == ' ') {
                            valid = 0;
                            Password.setError("White spaces are not allowed");
                            break;
                        }
                    }
                } else
                    Password.setError(null);


                if (valid == 1) {
                    s_progress.setVisibility(View.VISIBLE);
                            /*db.collection("QuizzerClassUsers").document(phone).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Toast.makeText(quiz.this, "You have already registered  " + documentSnapshot.getString("Sem"), Toast.LENGTH_LONG).show();
                                    s_progress.setVisibility(View.GONE);
                                    L_sign_up.setVisibility(View.GONE);
                                    in_signup = false;
                                    login.setVisibility(View.VISIBLE);

                                    Name.setText("");
                                    Sem.setText("");
                                    Phone.setText("");
                                    Password.setText("");

                                    l_phone = findViewById(R.id.l_phone);
                                    l_password = findViewById(R.id.l_password);
                                    l_phone.setText("");
                                    l_phone.setError(null);
                                    l_password.setText("");
                                    l_password.setError(null);
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {*/

                    user = new HashMap<>();
                    user.put("Name", name);
                    user.put("Sem", sem);
                    user.put("Password", password);

                    db.collection("QuizzerClassUsers").document(phone).update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(quiz.this, "User Updated", Toast.LENGTH_SHORT).show();
                            hideKeyboard();
                            s_progress.setVisibility(View.GONE);
                            L_sign_up.setVisibility(View.GONE);
                            subjects.setVisibility(View.VISIBLE);
                            subjectsListLayout();

                            Name.setText("");
                            Sem.setText("");
                            Phone.setText("");
                            Password.setText("");

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(quiz.this, "User not Updated " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            s_progress.setVisibility(View.GONE);
                        }
                    });


                }
                        /*} else {
                            s_progress.setVisibility(View.VISIBLE);
                            Phone_Otp = findViewById(R.id.phone_otp);
                            String otp = Phone_Otp.getText().toString();
                            if (!otp.isEmpty()) {
                                verifyCode(otp);
                            } else {
                                Phone_Otp.setError("Enter otp");
                                s_progress.setVisibility(View.GONE);
                            }*/

                            /*if (verified) {
                                Phone_Otp = findViewById(R.id.phone_otp);
                                String otp = Phone_Otp.getText().toString().trim();

                                if (!otp.isEmpty() && "123456".equals(otp)) {
                                    //Toast.makeText(quiz.this, "User Created", Toast.LENGTH_SHORT).show();

                                    db.collection("Users").document(phone).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(quiz.this, "User Created", Toast.LENGTH_SHORT).show();
                                            s_progress.setVisibility(View.GONE);
                                            L_sign_up.setVisibility(View.GONE);
                                            in_signup = false;
                                            login.setVisibility(View.VISIBLE);

                                            Name.setText("");
                                            College.setText("");
                                            branch.setText("");
                                            Email.setText("");
                                            Sem.setText("");
                                            Phone_Otp.setText("");
                                            Phone_Otp.setVisibility(View.GONE);
                                            reg.setText("VERIFY");
                                            Phone.setText("");
                                            Password.setText("");
                                            C_Password.setText("");

                                            l_phone = findViewById(R.id.l_phone);
                                            l_password = findViewById(R.id.l_password);
                                            l_phone.setText("");
                                            l_phone.setError(null);
                                            l_password.setText("");
                                            l_password.setError(null);

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(quiz.this, "User not Created " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            s_progress.setVisibility(View.GONE);

                                        }
                                    });
                                } else
                                    Toast.makeText(quiz.this, "Invalid OTP", Toast.LENGTH_SHORT).

                                            show();
                            }*/
            }
        });

        TextView have_acc = findViewById(R.id.have_acc);
        have_acc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                L_sign_up.setVisibility(View.GONE);
                in_signup = false;
                login.setVisibility(View.VISIBLE);

                l_phone = findViewById(R.id.l_phone);
                l_password = findViewById(R.id.l_password);

                Name = findViewById(R.id.name);
                Phone = findViewById(R.id.phone);
                Sem = findViewById(R.id.sem);
                //Phone_Otp = findViewById(R.id.phone_otp);
                Password = findViewById(R.id.password);

                Name.setText("");
                Name.setError(null);
                Sem.setText("");
                Sem.setError(null);
                Phone.setText("");
                Phone.setError(null);
                //reg.setText("VERIFY");
                Password.setText("");
                Password.setError(null);

                l_phone.setText("");
                l_phone.setError(null);
                l_password.setText("");
                l_password.setError(null);
            }
        });


    }

    private void startQuizLayout() {

        start_quiz = findViewById(R.id.start_quiz);
        start_quiz.setVisibility(View.VISIBLE);
        startQuizPage = true;

        TextView rules = findViewById(R.id.rules);
        rules.setText(Html.fromHtml("<u><b><strong>INSTRUCTIONS:</strong></b></u> <br> &nbsp &nbsp 1. Once you press START QUIZ the quiz starts with timer of time remaining to end quiz<br>" +
                "&nbsp &nbsp 2. Once your Quiz starts you SHOULD NOT PRESS HOME, BACK, MENU, POWER BUTTONS <br>" +
                "&nbsp &nbsp 3. If you press any of the above mentioned buttons or if you come out of the app QUIZ SUBMITS AUTOMATICALLY <br>" +
                "&nbsp &nbsp 4. Press SUBMIT button after attending ALL SUBJECTS <br>" +
                "&nbsp &nbsp 5. Check all the questions, If any of the questions not loaded, please sign in again"));

        CardView SQuiz = findViewById(R.id.SQuiz);
        SQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!entered) {
                    try {
                        if (Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME) == 1 && Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME_ZONE) == 1) {
                            // Enabled
                            final Calendar calendar = Calendar.getInstance();
                            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


                            db.collection("QuizzerClassTimestamp").document(selectedSubject).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    //testScheduleded = true;

                                    if (task.isSuccessful()) {
                                        DocumentSnapshot doc = task.getResult();
                                        if (doc.exists()) {
                                            String startt = doc.get("start").toString();
                                            stop = doc.get("stop").toString();
                                            try {
                                                if (calendar.getTime().compareTo(simpleDateFormat.parse(startt)) >= 0 && calendar.getTime().compareTo(simpleDateFormat.parse(stop)) < 0) {
                                                    mTimeLeftInMillis = simpleDateFormat.parse(stop).getTime() - simpleDateFormat.parse(simpleDateFormat.format(calendar.getTime())).getTime() + 60000;
                                                    start_quiz.setVisibility(View.GONE);
                                                    LinearLayout quiz = findViewById(R.id.quiz);
                                                    quiz.setVisibility(View.VISIBLE);
                                                    startQuizPage = false;
                                                    startTimer();
                                                    start = true;

                                                } else {
                                                    Toast.makeText(quiz.this, "you came before or after scheduled time", Toast.LENGTH_SHORT).show();
                                                }
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        } else
                                            Toast.makeText(quiz.this, "test not yet scheduled", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } else {
                            // Disabed
                            AlertDialog.Builder builder = new AlertDialog.Builder(quiz.this);
                            builder.setMessage("Set to Automatic date, time and time zone");
                            builder.setCancelable(false)
                                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS));
                                        }
                                    });
                            final AlertDialog dialog = builder.create();

                            //show dialog box
                            dialog.show();
                        }
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                } else
                    Toast.makeText(quiz.this, "you have already attended this subject", Toast.LENGTH_SHORT).show();
                /*start_quiz.setVisibility(View.GONE);
                quiz = findViewById(R.id.quiz);
                quiz.setVisibility(View.VISIBLE);
                try {
                    startTimer();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                start = true;*/

            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*private Boolean sendSMS(String phone) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            //String otp_code = String.valueOf(phone.charAt(2)) + String.valueOf(phone.charAt(6)) + String.valueOf(phone.charAt(9));
            int d = phone.charAt(2);
            int f = phone.charAt(5);
            int g = phone.charAt(9);

            smsManager.sendTextMessage(phone, null, "GM-QUIZZER sign up otp - " + String.valueOf(d) + String.valueOf(f) + String.valueOf(g), null, null);
            Toast.makeText(getApplicationContext(), "SMS sent. check you would have got failed sms to " + phone,
                    Toast.LENGTH_LONG).show();

            Name = findViewById(R.id.name);
            College = findViewById(R.id.college);
            branch = findViewById(R.id.branch);
            Email = findViewById(R.id.email);
            Sem = findViewById(R.id.sem);
            Phone = findViewById(R.id.phone);
            Password = findViewById(R.id.password);
            C_Password = findViewById(R.id.c_password);

            Phone_Otp = findViewById(R.id.phone_otp);
            Phone_Otp.setVisibility(View.VISIBLE);
            s_progress.setVisibility(View.GONE);
            reg.setText("REGISTER");

            Name.setEnabled(false);
            College.setEnabled(false);
            branch.setEnabled(false);
            Email.setEnabled(false);
            Sem.setEnabled(false);
            Phone.setEnabled(false);
            Password.setEnabled(false);
            C_Password.setEnabled(false);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "SMS not sent.",
                    Toast.LENGTH_LONG).show();
            s_progress.setVisibility(View.GONE);
            return false;
        }

    }*/

    private void startTimer() throws ParseException {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
            }
        }.start();

        /*Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (calendar.getTime().compareTo(simpleDateFormat.parse(stop)) == 0) {
            Toast.makeText(quiz.this, "your quiz is submitted", Toast.LENGTH_SHORT).show();
            validate_quiz();
        }*/
    }

    private void updateCountDownText() {

        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        int minutes = (int) ((mTimeLeftInMillis / (1000 * 60)) % 60);
        int hours = (int) ((mTimeLeftInMillis / (1000 * 60 * 60)) % 24);


        if (hours == 0 && minutes <= 0 && seconds <= 60) {
            mTextViewCountDown.setTextColor(Color.RED);
        }
        if (hours == 0 && minutes == 0 && seconds == 0) {
            validate_quiz();
        }
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        mTextViewCountDown.setText(timeLeftFormatted);
    }


    private void result() {

        db.collection("QuizzerClassUsers").document(s_phone).update(selectedSubject + "_marks", Result);

        createPDF();

        startActivity(new Intent(quiz.this, Resultt.class));
        finish();
    }

    private void createPDF() {

        PdfDocument myPdfDocument = new PdfDocument();
        Paint myPaint = new Paint();

        PdfDocument.PageInfo myPageInfo1 = new PdfDocument.PageInfo.Builder(400, 3 * 20 + (10 + totalQuestions) * 20, 1).create();
        PdfDocument.Page myPage1 = myPdfDocument.startPage(myPageInfo1);
        Canvas canvas = myPage1.getCanvas();

        int y_axis = 50;

        y_axis += 10;
        canvas.drawText(selectedSubject + ":", 20, y_axis, myPaint);
        y_axis += 20;

        for (int j = 0; j < totalQuestions; j++) {
            canvas.drawText(String.valueOf(j + 1) + ". " + Responses[j], 30, y_axis, myPaint);
            y_axis += 20;
        }

        myPdfDocument.finishPage(myPage1);

        File file = new File(Environment.getExternalStorageDirectory(), "/" + selectedSubject + "_Responses.pdf");

        try {
            myPdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(quiz.this, "Your quiz is submitted and response are stored in " + selectedSubject + "_Responses.pdf", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        myPdfDocument.close();
    }

    @Override
    public void onBackPressed() {

        if (startQuizPage) {
            startQuizPage = false;
            testScheduleded = false;
            start_quiz.setVisibility(View.GONE);
            subjectsListLayout();
        } else if (profile) {
            profile = false;
            L_sign_up.setVisibility(View.GONE);
            subjectsListLayout();
        } else if (in_signup) {
            L_sign_up.setVisibility(View.GONE);
            in_signup = false;
            login.setVisibility(View.VISIBLE);

            l_phone = findViewById(R.id.l_phone);
            l_password = findViewById(R.id.l_password);

            Name = findViewById(R.id.name);
            Sem = findViewById(R.id.sem);
            Phone = findViewById(R.id.phone);
            //Phone_Otp = findViewById(R.id.phone_otp);
            Password = findViewById(R.id.password);

            Name.setText("");
            Name.setError(null);
            Sem.setText("");
            Sem.setError(null);
            Phone.setText("");
            Phone.setError(null);
            Password.setText("");
            Password.setError(null);

            l_phone.setText("");
            l_phone.setError(null);
            l_password.setText("");
            l_password.setError(null);
        } else if (!start) {
            if (back)
                super.onBackPressed();
            else
                showDialog();
        } else {
        }

    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(quiz.this);
        builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        back = true;
                        onBackPressed();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog dialog = builder.create();

        //show dialog box
        dialog.show();

    }


    @Override
    protected void onPause() {
        super.onPause();
        if (start && call) {
            if (!storage_permission)
                Toast.makeText(this, "your quiz is submitted", Toast.LENGTH_LONG).show();
            validate_quiz();
            /*Toast.makeText(quiz.this,"comback to quiz within 5 seconds",Toast.LENGTH_SHORT).show();
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    // Do what ever you want
                    if (!storage_permission)
                        Toast.makeText(quiz.this, "your quiz is submitted", Toast.LENGTH_LONG).show();
                    validate_quiz();
                }
            };
            handler.postDelayed(runnable, 5000);*/
        }

        call = true;


    }


    @Override
    protected void onStart() {
        super.onStart();
        /*if(start)
            handler.removeCallbacks(runnable);*/


        if (start) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // to hide navigation bar during quiz
                    View quizPage = findViewById(R.id.quiz);
                    quizPage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }, 5000);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull final int[] grantResults) {

        // not checked request code because there only one permission requested in this activity(referenced)
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(quiz.this, "make sure no phone call comes during test", Toast.LENGTH_LONG).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(quiz.this, "your test responses will not be downloded", Toast.LENGTH_LONG).show();
                            storage_permission = false;
                        }
                    }
                }, 3500);
            } else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(quiz.this, "your test responses will not be downloded", Toast.LENGTH_LONG).show();
                storage_permission = false;
            }

        }
    }
}

