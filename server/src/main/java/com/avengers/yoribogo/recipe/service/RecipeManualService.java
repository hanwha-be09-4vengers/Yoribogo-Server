package com.avengers.yoribogo.recipe.service;

import com.avengers.yoribogo.recipe.dto.RecipeManualDTO;
import com.avengers.yoribogo.recipe.dto.RequestRecipeManualDTO;

import java.util.List;

public interface RecipeManualService {

    // 요리 레시피 아이디로 매뉴얼 조회
    List<RecipeManualDTO> findRecipeManualByRecipeId(Long recipeId);

    // 요리 레시피 매뉴얼 등록
    List<RecipeManualDTO> registRecipeManual(Long recipeId, RequestRecipeManualDTO requestRecipeManualDTO);

    // 요리 레시피 매뉴얼 수정
    List<RecipeManualDTO> modifyRecipeManual(Long recipeId, RequestRecipeManualDTO requestRecipeManualDTO);

}
