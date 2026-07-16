package com.nudo.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nudo.app.ItemHistorial
import com.nudo.app.R
import com.nudo.app.databinding.ItemConversacionBinding
import com.nudo.app.util.Fechas

class AdaptadorHistorial(
    private val alTocarItem: (ItemHistorial) -> Unit,
) : ListAdapter<ItemHistorial, AdaptadorHistorial.Sujetador>(Diferencias) {

    class Sujetador(val binding: ItemConversacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int): Sujetador {
        val binding = ItemConversacionBinding.inflate(
            LayoutInflater.from(padre.context), padre, false,
        )
        return Sujetador(binding)
    }

    override fun onBindViewHolder(sujetador: Sujetador, posicion: Int) {
        val item = getItem(posicion)
        val binding = sujetador.binding
        val contexto = binding.root.context

        when (item) {
            is ItemHistorial.Remoto -> {
                binding.tituloItem.text =
                    item.trabajo.titulo ?: contexto.getString(R.string.sin_titulo)
                binding.metaItem.text = Fechas.formatearCreado(item.trabajo.creado)
                when (item.trabajo.estado) {
                    "completado" -> pintarPildora(binding, R.string.estado_atada, R.color.nudo_atada, R.color.nudo_atada_tenue)
                    "error" -> pintarPildora(binding, R.string.estado_fallo, R.color.nudo_hilo, R.color.nudo_hilo_tenue)
                    else -> pintarPildora(binding, R.string.estado_anudando, R.color.nudo_anudando, R.color.nudo_anudando_tenue)
                }
            }
            is ItemHistorial.Pendiente -> {
                binding.tituloItem.text = contexto.getString(R.string.sin_titulo)
                binding.metaItem.text = Fechas.formatearInstante(item.archivo.lastModified())
                pintarPildora(binding, R.string.estado_sin_subir, R.color.nudo_hilo, R.color.nudo_hilo_tenue)
            }
        }
        binding.root.setOnClickListener { alTocarItem(item) }
    }

    private fun pintarPildora(binding: ItemConversacionBinding, texto: Int, colorTexto: Int, colorFondo: Int) {
        val contexto = binding.root.context
        binding.pildoraItem.setText(texto)
        binding.pildoraItem.setTextColor(contexto.getColor(colorTexto))
        binding.pildoraItem.backgroundTintList =
            ColorStateList.valueOf(contexto.getColor(colorFondo))
    }

    private object Diferencias : DiffUtil.ItemCallback<ItemHistorial>() {
        override fun areItemsTheSame(a: ItemHistorial, b: ItemHistorial): Boolean = clave(a) == clave(b)

        override fun areContentsTheSame(a: ItemHistorial, b: ItemHistorial): Boolean = a == b

        private fun clave(item: ItemHistorial): String = when (item) {
            is ItemHistorial.Remoto -> "remoto:${item.trabajo.id}"
            is ItemHistorial.Pendiente -> "pendiente:${item.archivo.name}"
        }
    }
}
