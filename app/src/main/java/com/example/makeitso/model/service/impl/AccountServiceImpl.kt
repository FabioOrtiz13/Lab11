/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.example.makeitso.model.service.impl

import android.util.Log
import com.example.makeitso.model.User
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.trace
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AccountServiceImpl @Inject constructor(private val auth: FirebaseAuth) : AccountService {

  override val currentUserId: String
    get() = auth.currentUser?.uid.orEmpty()

  override val hasUser: Boolean
    get() = auth.currentUser != null

  override val currentUser: Flow<User>
    get() = callbackFlow {
      val listener =
        FirebaseAuth.AuthStateListener { auth ->
          this.trySend(auth.currentUser?.let { User(it.uid, it.isAnonymous) } ?: User())
        }
      auth.addAuthStateListener(listener)
      awaitClose { auth.removeAuthStateListener(listener) }
    }

  override suspend fun authenticate(email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password).await()
  }

  override suspend fun sendRecoveryEmail(email: String) {
    auth.sendPasswordResetEmail(email).await()
  }

  override suspend fun createAnonymousAccount() {
    auth.signInAnonymously().await()
  }

  override suspend fun linkAccount(email: String, password: String) {
    try {
      val user = auth.currentUser
      if (user == null) {
        Log.e("AccountService", "No hay usuario autenticado para vincular")
        throw IllegalStateException("Usuario no autenticado")
      }
      Log.d("AccountService", "Usuario actual: ${user.uid}, isAnonymous: ${user.isAnonymous}")
      val credential = EmailAuthProvider.getCredential(email, password)
      user.linkWithCredential(credential).await()
      Log.d("AccountService", "Cuenta vinculada correctamente")
    } catch (e: Exception) {
      Log.e("AccountService", "Error al vincular cuenta: ${e.message}", e)
      throw e
    }
  }


  override suspend fun deleteAccount() {
    auth.currentUser!!.delete().await()
  }

  override suspend fun signOut() {

    auth.signOut()


  }

  companion object {
    private const val LINK_ACCOUNT_TRACE = "linkAccount"
  }
}
