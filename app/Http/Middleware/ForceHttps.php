<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class ForceHttps
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        // HTTPS 강제 설정
        if (!$request->isSecure() && $request->header('X-Forwarded-Proto') !== 'https') {
            $request->server->set('HTTPS', 'on');
            $request->server->set('SERVER_PORT', '9000');
        }
        
        // URL 스키마 강제 (forceRootUrl 제거 - Chrome "위험한 사이트" 문제 해결)
        \URL::forceScheme('https');
        // \URL::forceRootUrl('https://natus250601.viewdns.net:9000'); // 제거
        
        return $next($request);
    }
}