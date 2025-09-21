<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use Illuminate\Foundation\Auth\AuthenticatesUsers;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;

class LoginController extends Controller
{
    /*
    |--------------------------------------------------------------------------
    | Login Controller
    |--------------------------------------------------------------------------
    |
    | This controller handles authenticating users for the application and
    | redirecting them to your home screen. The controller uses a trait
    | to conveniently provide its functionality to your applications.
    |
    */

    use AuthenticatesUsers;

    /**
     * Where to redirect users after login.
     *
     * @var string
     */
    protected $redirectTo = '/main';
    
    /**
     * Get the failed login response instance.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Symfony\Component\HttpFoundation\Response
     *
     * @throws \Illuminate\Validation\ValidationException
     */
    protected function sendFailedLoginResponse(Request $request)
    {
        throw ValidationException::withMessages([
            $this->username() => [trans('auth.failed')],
        ])->redirectTo('/login');
    }
    
    /**
     * Get the post-login redirect path.
     *
     * @return string
     */
    public function redirectPath()
    {
        return '/main';
    }

    /**
     * Create a new controller instance.
     *
     * @return void
     */
    public function __construct()
    {
        $this->middleware('guest')->except('logout');
        $this->middleware('auth')->only('logout');
    }

    /**
     * The user has been authenticated.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  mixed  $user
     * @return mixed
     */
    protected function authenticated(Request $request, $user)
    {
        // 최종 접속일 업데이트
        $user->update(['last_login_at' => now()]);
        
        // 로그인 환영 메시지 설정
        session(['welcome_message' => $user->name . '님, 다시 오신 것을 환영합니다! 👋']);
        
        // 로그인 정보 기억하기가 체크되어 있으면 이메일과 체크박스 상태를 쿠키에 저장
        if ($request->filled('remember')) {
            cookie()->queue('remember_email', $request->email, 60 * 24 * 30); // 30일간 저장
            cookie()->queue('remember_me', '1', 60 * 24 * 30); // 30일간 저장
        } else {
            // 체크되지 않았으면 쿠키 삭제
            cookie()->queue(cookie()->forget('remember_email'));
            cookie()->queue(cookie()->forget('remember_me'));
        }
        
        // 로그인 성공 시 무조건 /main으로 리다이렉트 (위험한 사이트 문제 해결)
        return redirect('/main');
    }

    /**
     * Show the application's login form (login2).
     *
     * @return \Illuminate\View\View
     */
    public function showLoginForm2()
    {
        return view('auth.login2');
    }

    /**
     * Log the user out of the application.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\RedirectResponse
     */
    public function logout(Request $request)
    {
        $this->guard()->logout();

        $request->session()->invalidate();

        $request->session()->regenerateToken();

        return $this->loggedOut($request) ?: redirect('/');
    }
}
