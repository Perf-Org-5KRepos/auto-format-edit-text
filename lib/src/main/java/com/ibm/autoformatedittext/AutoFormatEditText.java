package com.ibm.autoformatedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.databinding.BindingAdapter;

import com.carljmont.lib.R;

public class AutoFormatEditText extends AbstractAutoEditText {
    private static final char DEFAULT_PLACEHOLDER = '#';
    private InputMask inputMask;

    public AutoFormatEditText(Context context) {
        super(context);
    }

    public AutoFormatEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoFormatEditText);
            String inputMaskString = a.getString(R.styleable.AutoFormatEditText_inputMask);
            String formatPlaceholder = a.getString(R.styleable.AutoFormatEditText_placeholder);
            a.recycle();

            char placeholder = DEFAULT_PLACEHOLDER;
            if (formatPlaceholder != null && formatPlaceholder.length() > 0) {
                placeholder = formatPlaceholder.charAt(0);
            }

            inputMask = new InputMask(inputMaskString, placeholder);
        }
    }

    private void setInputMask(String inputMaskString) {
        String unformattedText = getUnformattedText();
        inputMask.setInputMaskString(inputMaskString);

        //If length of unformatted text is smaller than the input mask, it must be trimmed
        if (unformattedText != null && unformattedText.length() > inputMaskString.length()) {
            unformattedText = unformattedText.substring(0, inputMaskString.length());
        }

        setText(unformattedText); //Will cause re-formatting
    }


    @Override
    EditTextState onInputChanged(String textBefore, String textAfter, int selectionStart, int selectionLength, int replacementLength) {
        //Case where no input mask exists, so the text can be entered without restriction
        if (inputMask == null || inputMask.getInputMaskString() == null || inputMask.getInputMaskString().isEmpty()) {
            return new EditTextState(textAfter, textAfter, selectionStart + replacementLength);
        }

        //Case where user is attempting to enter text beyond the length of the input mask
        if (textAfter.length() > inputMask.getInputMaskString().length()
            && selectionLength != replacementLength && selectionStart > 0
            && !inputMask.matches(textAfter)) {
                String newUnformattedText = inputMask.unformatText(textBefore, 0, textBefore.length());
                return new EditTextState(textBefore, newUnformattedText, selectionStart);
        }

        CharSequence insertedText = textAfter.subSequence(selectionStart, selectionStart + replacementLength);
        String leftUnformatted = inputMask.unformatText(textBefore, 0, selectionStart);
        String rightUnformatted = inputMask.unformatText(textBefore, selectionStart + selectionLength, textBefore.length());

        //Special case where user has backspaced in front of a character added by the input mask
        //Remove next character to the left
        if (leftUnformatted.length() > 0 &&
                leftUnformatted.length() <= inputMask.getUnformattedLength() &&
                !inputMask.isPlaceholder(selectionStart) &&
                selectionLength == 1 && replacementLength == 0) {
            leftUnformatted = leftUnformatted.substring(0, leftUnformatted.length() - 1);
        }

        String newUnformattedText = leftUnformatted + insertedText + rightUnformatted;
        String newFormattedText = inputMask.formatText(newUnformattedText);
        int cursorPos = inputMask.formatText(leftUnformatted + insertedText).length();

        return new EditTextState(newFormattedText, newUnformattedText, cursorPos);
    }

    @BindingAdapter("inputMask")
    public static void setInputMask(AutoFormatEditText editText, String inputMaskString) {
        editText.setInputMask(inputMaskString);
    }
}