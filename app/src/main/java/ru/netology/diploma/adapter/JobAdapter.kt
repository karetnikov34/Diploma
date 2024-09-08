package ru.netology.diploma.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.diploma.databinding.FragmentOneJobCardBinding
import ru.netology.diploma.dto.Job
import ru.netology.diploma.util.formatDateTimeJobBinding

interface OnInteractionListenerJob {
    fun editJob(job: Job)
    fun removeJob(job: Job)
}

class JobAdapter(private val onInteractionListener: OnInteractionListenerJob) :
    ListAdapter<Job, JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding =
            FragmentOneJobCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }
}

class JobViewHolder(
    private val binding: FragmentOneJobCardBinding,
    private val onInteractionListener: OnInteractionListenerJob
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(job: Job) {
        binding.apply {

            editButton.isVisible = job.ownedByMe
            removeButton.isVisible = job.ownedByMe

            company.text = job.name
            position.text = job.position
            start.text = formatDateTimeJobBinding (job.start)
            finish.text = job.finish?.let { formatDateTimeJobBinding (it) } ?: ""
            link.text = job.link

            link.isVisible = (job.link != null)

            editButton.setOnClickListener {
                onInteractionListener.editJob(job)
            }

            removeButton.setOnClickListener {
                onInteractionListener.removeJob(job)
            }

        }
    }
}

class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem == newItem
    }
}