<?php

namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use Laravel\Socialite\Facades\Socialite;
use Illuminate\Pagination\Paginator;
use Illuminate\Support\Facades\View;
use App\Models\BoardType;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // HTTPS 강제 설정 (프록시 환경에서)
        if (env('FORCE_HTTPS', false)) {
            \URL::forceScheme('https');
        }
        
        // 페이징 뷰로 Bootstrap 5 사용
        Paginator::useBootstrapFive();

        // 모든 뷰에서 게시판 타입 목록을 공유
        View::composer('layouts.app', function ($view) {
            $boardTypes = BoardType::where('is_active', true)
                ->orderBy('sort_order')
                ->orderBy('name')
                ->get();
            $view->with('sharedBoardTypes', $boardTypes);
            
            // 현재 게시판 타입 정보도 공유
            $currentBoardTypeSlug = request()->route('boardType');
            if ($currentBoardTypeSlug) {
                $currentBoardType = BoardType::where('slug', $currentBoardTypeSlug)->first();
                if ($currentBoardType) {
                    $view->with('boardType', $currentBoardType);
                }
            }
        });

        // Kakao Socialite 드라이버 추가
        Socialite::extend('kakao', function ($app) {
            $config = $app['config']['services.kakao'];
            return new \App\Socialite\KakaoProvider(
                $app['request'],
                $config['client_id'],
                $config['client_secret'],
                $config['redirect']
            );
        });

        // Naver Socialite 드라이버 추가
        Socialite::extend('naver', function ($app) {
            $config = $app['config']['services.naver'];
            return new \App\Socialite\NaverProvider(
                $app['request'],
                $config['client_id'],
                $config['client_secret'],
                $config['redirect']
            );
        });
    }
}
