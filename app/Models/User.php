<?php

namespace App\Models;

// use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;
use PHPOpenSourceSaver\JWTAuth\Contracts\JWTSubject;

class User extends Authenticatable implements JWTSubject
{
    use HasApiTokens, HasFactory, Notifiable;

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'name',
        'email',
        'password',
        'is_admin',
        'provider',
        'provider_id',
        'avatar',
        'nickname',
        'last_login_at',
        'total_ddongsun_power',
        'current_level',
        'profile_image',
    ];

    /**
     * The attributes that should be hidden for serialization.
     *
     * @var array<int, string>
     */
    protected $hidden = [
        'password',
        'remember_token',
    ];

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'email_verified_at' => 'datetime',
        'password' => 'hashed',
        'is_admin' => 'boolean',
    ];

    public function boards()
    {
        return $this->hasMany(\App\Models\Board::class);
    }

    public function puzzleGames()
    {
        return $this->hasMany(\App\Models\UserPuzzleGame::class);
    }

    public function puzzleProfiles()
    {
        return $this->hasMany(\App\Models\UserPuzzleProfile::class);
    }

    public function puzzleGameRecords()
    {
        return $this->hasMany(\App\Models\PuzzleGameRecord::class);
    }

    public function lottoTickets()
    {
        return $this->hasMany(LottoTicket::class);
    }

    public function ddongsunRankings()
    {
        return $this->hasMany(DdongsunRanking::class);
    }

    /**
     * Get the identifier that will be stored in the subject claim of the JWT.
     *
     * @return mixed
     */
    public function getJWTIdentifier()
    {
        return $this->getKey();
    }

    /**
     * Return a key value array, containing any custom claims to be added to the JWT.
     *
     * @return array
     */
    public function getJWTCustomClaims()
    {
        return [];
    }

    /**
     * Check if user is admin
     *
     * @return bool
     */
    public function isAdmin()
    {
        return $this->is_admin === true;
    }

    /**
     * Check if user is specific admin by email
     *
     * @param string $email
     * @return bool
     */
    public function isSpecificAdmin($email)
    {
        return $this->email === $email && $this->is_admin === true;
    }
}
