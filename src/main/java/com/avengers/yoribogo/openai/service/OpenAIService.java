package com.avengers.yoribogo.openai.service;

import com.avengers.yoribogo.openai.dto.ResponseChatDTO;
import com.avengers.yoribogo.openai.dto.ResponseImagesDTO;

public interface OpenAIService {

    ResponseChatDTO getRecommend(String prompt);

    ResponseImagesDTO getImages(String prompt);

}
