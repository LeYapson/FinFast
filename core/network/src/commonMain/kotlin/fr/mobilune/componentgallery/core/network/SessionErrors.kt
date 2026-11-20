package fr.mobilune.componentgallery.core.network

/** 401 (jeton absent/expiré) ou 403 (jeton refusé) : la session n'est plus valable. */
fun Int.isSessionRejection(): Boolean = this == 401 || this == 403
