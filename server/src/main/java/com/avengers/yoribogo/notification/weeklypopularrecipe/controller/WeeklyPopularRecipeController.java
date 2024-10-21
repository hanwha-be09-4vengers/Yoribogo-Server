package com.avengers.yoribogo.notification.weeklypopularrecipe.controller;

import com.avengers.yoribogo.common.ResponseDTO;
import com.avengers.yoribogo.notification.weeklypopularrecipe.dto.WeeklyPopularRecipe;
import com.avengers.yoribogo.notification.weeklypopularrecipe.service.WeeklyPopularRecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weeklypopularrecipes")
public class WeeklyPopularRecipeController {

    @Autowired
    private final WeeklyPopularRecipeService weeklyPopularRecipeService;

    public WeeklyPopularRecipeController(WeeklyPopularRecipeService weeklyPopularRecipeService) {
        this.weeklyPopularRecipeService = weeklyPopularRecipeService;
    }

    // MongoDB 연결 테스트용 API
    @GetMapping("/findall")
    public ResponseEntity<ResponseDTO<List<WeeklyPopularRecipe>>> getAllWeeklyPopularRecipes() {
        List<WeeklyPopularRecipe> recipes = weeklyPopularRecipeService.getAllWeeklyPopularRecipes();
        return ResponseEntity.ok(ResponseDTO.ok(recipes));
    }

    // MongoDB에서 가장 많은 좋아요를 가진 레시피 추출하는 API (추후에 일주일 타임 걸어두는 로직 추가 필요)
    @GetMapping("/mostliked")
    public ResponseEntity<ResponseDTO<WeeklyPopularRecipe>> getMostLikedRecipe() {
        WeeklyPopularRecipe recipe = weeklyPopularRecipeService.getRandomTopLikedRecipe();
        return ResponseEntity.ok(ResponseDTO.ok(recipe));
    }
}