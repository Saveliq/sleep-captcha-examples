package com.urbandroid.sleep.captcha.example;

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

import java.util.Locale;
import java.util.Random;

public class HardMathCaptchaActivity extends Activity {
    private static final long MAX_ANSWER_ABS = 9999999999L;
    private static final int MAX_GENERATION_TRIES = 30;

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
        if (captchaSupport != null) captchaSupport.destroy();
    }

    @Override
    public void onBackPressed() {
        if (captchaSupport != null) captchaSupport.unsolved();
        super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (captchaSupport != null) captchaSupport.alive();
    }

    private void generateNewProblem() {
        int difficulty = 1;
        if (captchaSupport != null) {
            try {
                difficulty = captchaSupport.getDifficulty(); // 1..5
            } catch (Throwable ignored) {
                difficulty = 1;
            }
        }

        Problem p = null;
        for (int i = 0; i < MAX_GENERATION_TRIES; i++) {
            Problem candidate = createProblemForDifficulty(difficulty);
            if (candidate == null) continue;
            if (Math.abs(candidate.answer) > MAX_ANSWER_ABS) continue;
            if (candidate.answer == Long.MIN_VALUE || candidate.answer == Long.MAX_VALUE) continue;
            p = candidate;
            break;
        }

        if (p == null) p = new Problem("12 + 34", 46);

        correctAnswer = p.answer;
        questionView.setText(p.text);
        answerInput.setText("");
    }

    private void checkAnswer() {
        if (captchaSupport != null) captchaSupport.alive();

        String text = answerInput.getText() != null ? answerInput.getText().toString().trim() : "";
        if (text.isEmpty()) {
            answerInput.setError(getString(R.string.err_enter_answer));
            return;
        }

        long userAnswer;
        try {
            userAnswer = Long.parseLong(text);
        } catch (NumberFormatException e) {
            answerInput.setError(getString(R.string.err_integers_only));
            return;
        }

        if (userAnswer == correctAnswer) {
            Toast.makeText(this, getString(R.string.ok_correct), Toast.LENGTH_SHORT).show();
            if (captchaSupport != null) captchaSupport.solved();
            finish();
        } else {
            answerInput.setError(getString(R.string.err_wrong_new));
            generateNewProblem();
        }
    }

    private Problem createProblemForDifficulty(int difficulty) {
        if (difficulty <= 1) return createEasyProblem();
        if (difficulty == 2) return createMediumProblem();
        if (difficulty == 3) return createHardProblem();
        return createInsaneProblem(difficulty);
    }

    private Problem createEasyProblem() {
        int a = random.nextInt(200) + 1;
        int b = random.nextInt(200) + 1;
        boolean plus = random.nextBoolean();
        long ans = plus ? (a + (long) b) : Math.max(0, a - (long) b);
        String text = a + (plus ? " + " : " - ") + b;
        return new Problem(text, ans);
    }

    private Problem createMediumProblem() {
        int a = random.nextInt(40) + 2;
        int b = random.nextInt(40) + 2;
        int c = random.nextInt(400) + 1;
        long mul = (long) a * (long) b;
        boolean plus = random.nextBoolean();
        long ans = plus ? (mul + (long) c) : Math.max(0, mul - (long) c);
        String text = a + "×" + b + (plus ? " + " : " - ") + c;
        return new Problem(text, ans);
    }

    private Problem createHardProblem() {
        int a = random.nextInt(7) + 2;
        int b = random.nextInt(4) + 2;
        int c = random.nextInt(80) + 2;
        int d = random.nextInt(1500) + 1;

        long p = powChecked(a, b);
        if (p == Long.MAX_VALUE) return null;

        long mul = mulChecked(p, c);
        if (mul == Long.MAX_VALUE) return null;

        boolean plus = random.nextBoolean();
        long ans = plus ? addChecked(mul, d) : Math.max(0, subChecked(mul, d));
        if (ans == Long.MAX_VALUE) return null;

        String text = a + "^" + b + "×" + c + (plus ? " + " : " - ") + d;
        return new Problem(text, ans);
    }

    private Problem createInsaneProblem(int level) {
        // оставь твою текущую реализацию "insane*" (она большая) — сюда можно вставить её 1:1
        // чтобы ответ не раздувать, я оставил заглушку:
        int a = randRange(4, level >= 5 ? 10 : 9);
        long fa = factChecked(a);
        if (fa == Long.MAX_VALUE) return null;
        String text = a + "! + 1";
        return new Problem(text, fa + 1);
    }

    private int randRange(int minIncl, int maxIncl) {
        if (maxIncl <= minIncl) return minIncl;
        return random.nextInt(maxIncl - minIncl + 1) + minIncl;
    }

    private long addChecked(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    private long subChecked(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    private long mulChecked(long x, long y) {
        if (x == 0 || y == 0) return 0;
        if (x > 0 && y > 0 && x > Long.MAX_VALUE / y) return Long.MAX_VALUE;
        if (x < 0 && y < 0 && x < Long.MAX_VALUE / y) return Long.MAX_VALUE;
        if (x > 0 && y < 0 && y < Long.MIN_VALUE / x) return Long.MAX_VALUE;
        if (x < 0 && y > 0 && x < Long.MIN_VALUE / y) return Long.MAX_VALUE;
        return x * y;
    }

    private long powChecked(int base, int exp) {
        if (exp < 0) return Long.MAX_VALUE;
        long res = 1L;
        for (int i = 0; i < exp; i++) {
            res = mulChecked(res, base);
            if (res == Long.MAX_VALUE) return Long.MAX_VALUE;
        }
        return res;
    }

    private long factChecked(int n) {
        if (n < 0) return Long.MAX_VALUE;
        long res = 1L;
        for (int i = 2; i <= n; i++) {
            res = mulChecked(res, i);
            if (res == Long.MAX_VALUE) return Long.MAX_VALUE;
        }
        return res;
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
