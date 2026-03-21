import { defineStore } from 'pinia';
import axios from 'axios';

const AUTH_API_URL = process.env.VUE_APP_API_BASE
  ? `${process.env.VUE_APP_API_BASE}/auth/google-login`
  : '/auth/google-login';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // Initialize state properties from localStorage
    jwtToken: localStorage.getItem('access_token') || null,
    isSignedIn: !!localStorage.getItem('access_token'), // Derived from jwtToken presence
    message: null,
    loading: false,

    // User-specific data to be persisted
    userName: localStorage.getItem('user_name') || null,
    userEmail: localStorage.getItem('user_email') || null,
    userPicture: localStorage.getItem('user_picture') || null,
    userHd: localStorage.getItem('user_hd') || null,
    // Store the entire user object as a JSON string, then parse it back
    user: JSON.parse(localStorage.getItem('user_data')) || null,
  }),

  actions: {
    async handleGoogleLogin(userData) {
      this.loading = true;
      this.message = null;

      // Clear all existing state and local storage items before a new login attempt
      // This ensures a clean slate if a previous session was incomplete or corrupted.
      this.jwtToken = null;
      this.isSignedIn = false;
      this.userName = null;
      this.userEmail = null;
      this.userPicture = null;
      this.userHd = null;
      this.user = null;
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_name');
      localStorage.removeItem('user_email');
      localStorage.removeItem('user_picture');
      localStorage.removeItem('user_hd');
      localStorage.removeItem('user_data');


      try {
        const response = await axios.post(AUTH_API_URL, { userData });

        // Store the JWT token received from your backend
        this.jwtToken = response.data.access_token;
        localStorage.setItem('access_token', this.jwtToken);
        this.isSignedIn = true;

        if (response.data.user) {
          // Update Pinia state with user details
          this.userName = response.data.user.name;
          this.userEmail = response.data.user.email;
          this.userPicture = response.data.user.picture;
          this.userHd = response.data.user.hd;
          this.user = response.data.user; // Store the entire `user` object

          // Persist user details to localStorage
          localStorage.setItem('user_name', this.userName);
          localStorage.setItem('user_email', this.userEmail);
          localStorage.setItem('user_picture', this.userPicture);
          localStorage.setItem('user_hd', this.userHd);
          localStorage.setItem('user_data', JSON.stringify(this.user)); // Store full object as JSON string
        }

        this.message = response.data.message || "Successfully signed in!";

      } catch (err) {
        console.error("Login failed:", err);
        this.message = err.response?.data?.message || 'Login failed. Please try again.';
        this.jwtToken = null;
        this.isSignedIn = false;
        // Ensure all persisted data is cleared on login failure as well
        localStorage.removeItem('access_token');
        localStorage.removeItem('user_name');
        localStorage.removeItem('user_email');
        localStorage.removeItem('user_picture');
        localStorage.removeItem('user_hd');
        localStorage.removeItem('user_data');
        throw err; // Re-throw to allow component to catch and show a toast
      } finally {
        this.loading = false;
      }
    },

    logout() {
      // Clear Pinia state
      this.jwtToken = null;
      this.isSignedIn = false;
      this.message = null;
      this.userName = null;
      this.userEmail = null;
      this.userPicture = null;
      this.userHd = null;
      this.user = null;

      // Clear all related data from localStorage
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_name');
      localStorage.removeItem('user_email');
      localStorage.removeItem('user_picture');
      localStorage.removeItem('user_hd');
      localStorage.removeItem('user_data');

      // Component will typically handle redirection after logout
    },

    // Optional: showAlertDialog action can be added here if it's meant to be part of the store's responsibilities
    // showAlertDialog(title, msg) {
    //   console.log(`Alert: ${title} - ${msg}`);
    // }
  },

  getters: {
    isLoggedIn: (state) => state.isSignedIn,
    currentUser: (state) => state.user, // Returns the full user object
    getUserName: (state) => state.userName,
    getUserEmail: (state) => state.userEmail,
    getUserPicture: (state) => state.userPicture,
    getUserHd: (state) => state.userHd,
  },
});