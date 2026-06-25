package com.unifurniture.mobile.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentChatBinding;
import com.unifurniture.mobile.ui.adapter.ChatAdapter;
import com.unifurniture.mobile.util.SessionManager;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true); // newest messages anchored to the bottom
        binding.recyclerMessages.setLayoutManager(lm);
        binding.recyclerMessages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.btnSend.setOnClickListener(v -> send());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            send();
            return true;
        });

        viewModel.getMessages().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.tvGreeting.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submit(list);
            if (!empty) binding.recyclerMessages.scrollToPosition(list.size() - 1);
        });

        viewModel.getSending().observe(getViewLifecycleOwner(), sending -> {
            boolean busy = Boolean.TRUE.equals(sending);
            binding.btnSend.setEnabled(!busy);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                // TODO temporary: show the real reason to diagnose the "can't reach" errors.
                Toast.makeText(requireContext(),
                        getString(R.string.chat_error) + "\n(" + msg + ")",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void send() {
        String text = binding.etMessage.getText() != null
                ? binding.etMessage.getText().toString() : "";
        if (text.trim().isEmpty()) return;
        binding.etMessage.setText("");

        String userId = SessionManager.getInstance(requireContext()).getCustomerId();
        if (userId == null || userId.isEmpty()) userId = "guest";

        viewModel.sendMessage(text, userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
