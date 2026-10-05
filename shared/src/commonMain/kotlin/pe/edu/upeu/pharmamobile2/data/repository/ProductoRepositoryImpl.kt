package pe.edu.upeu.pharmamobile2.data.repository

import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi

class ProductoRepositoryImpl(
    api: ProductoApi,
    categoriaPorDefecto: Long = 26L
) : ProductoRepositorioRest(api, categoriaPorDefecto)
