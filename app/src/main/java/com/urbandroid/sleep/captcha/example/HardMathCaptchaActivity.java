package com.urbandroid.sleep.captcha.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.urbandroid.sleep.captcha.CaptchaSupport;
import com.urbandroid.sleep.captcha.CaptchaSupportFactory;

import java.util.Random;

public class HardMathCaptchaActivity extends Activity {

    private CaptchaSupport captchaSupport;

    private TextView questionView;
    private EditText answerInput;
    private Button checkButton;

    private long correctAnswer;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_math_captcha);

        captchaSupport = CaptchaSupportFactory.create(this);

        questionView = findViewById(R.id.question);
        answerInput = findViewById(R.id.answer_input);
        checkButton = findViewById(R.id.check_button);

        generateNewProblem();

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captchaSupport = CaptchaSupportFactory.create(this, intent);
        generateNewProblem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captchaSupport != null) {
            captchaSupport.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (captchaSupport != null) {
            captchaSupport.unsolved();
        }
        super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (captchaSupport != null) {
            captchaSupport.alive();
        }
    }

    private void generateNewProblem() {
        int difficulty = 0;
        if (captchaSupport != null) {
            try {
                difficulty = captchaSupport.getDifficulty();
            } catch (Throwable ignored) {
                // на всякий случай, если что-то пойдёт не так
            }
        }

        Problem problem = createProblemForDifficulty(difficulty);
        correctAnswer = problem.answer;
        questionView.setText(problem.text);
        answerInput.setText("");
    }

    private void checkAnswer() {
        if (captchaSupport != null) {
            captchaSupport.alive();
        }

        String text = answerInput.getText().toString().trim();
        if (text.isEmpty()) {
            answerInput.setError("Введи ответ");
            return;
        }

        long userAnswer;
        try {
            userAnswer = Long.parseLong(text);
        } catch (NumberFormatException e) {
            answerInput.setError("Только целые числа");
            return;
        }

        if (userAnswer == correctAnswer) {
            Toast.makeText(this, "Верно", Toast.LENGTH_SHORT).show();
            if (captchaSupport != null) {
                captchaSupport.solved();
            }
            finish();
        } else {
            answerInput.setError("Неверно, новая задача");
            generateNewProblem();
        }
    }

    private Problem createProblemForDifficulty(int difficulty) {
        // 0 = easy, 1 = medium, 2+ = hard
        if (difficulty <= 0) {
            return createEasyProblem();
        } else if (difficulty == 1) {
            return createMediumProblem();
        } else {
            return createHardProblem();
        }
    }

    // a (+|-) b
    private Problem createEasyProblem() {
        int a = random.nextInt(50) + 1;   // 1..50
        int b = random.nextInt(50) + 1;   // 1..50
        boolean plus = random.nextBoolean();

        long ans;
        String text;
        if (plus) {
            ans = a + b;
            text = a + " + " + b;
        } else {
            ans = a - b;
            text = a + " - " + b;
        }

        return new Problem(text, ans);
    }

    // (a * b) (+|-) c
    private Problem createMediumProblem() {
        int a = random.nextInt(20) + 2;   // 2..21
        int b = random.nextInt(20) + 2;   // 2..21
        int c = random.nextInt(100) + 1;  // 1..100

        long mul = (long) a * (long) b;
        boolean plus = random.nextBoolean();

        long ans;
        String opOuter;
        if (plus) {
            ans = mul + c;
            opOuter = " + ";
        } else {
            ans = mul - c;
            opOuter = " - ";
        }

        String text = "(" + a + " × " + b + ")" + opOuter + c;
        return new Problem(text, ans);
    }

    // ((a^b) * c) (+|-) d  с ограничением по размеру результата
    private Problem createHardProblem() {
        int base = random.nextInt(7) + 2;   // 2..8
        int exp = random.nextInt(3) + 2;    // 2..4

        long pow = 1L;
        for (int i = 0; i < exp; i++) {
            pow *= base;
        }

        // немного ограничим, чтобы не уходить в оверфлоу при умножении
        int cMax = (int) Math.max(2, 1_000_000L / Math.max(1L, pow));
        if (cMax < 2) {
            cMax = 2;
        } else if (cMax > 50) {
            cMax = 50;
        }

        int c = random.nextInt(cMax - 1) + 2;  // 2..cMax
        long mul = pow * c;

        int d = random.nextInt(300) + 50;  // 50..349
        boolean plus = random.nextBoolean();

        long ans;
        String opOuter;
        if (plus) {
            ans = mul + d;
            opOuter = " + ";
        } else {
            ans = mul - d;
            opOuter = " - ";
        }

        String text = "( " + base + "^" + exp + " × " + c + " )" + opOuter + d;
        return new Problem(text, ans);
    }

    private static class Problem {
        final String text;
        final long answer;

        Problem(String text, long answer) {
            this.text = text;
            this.answer = answer;
        }
    }
}
